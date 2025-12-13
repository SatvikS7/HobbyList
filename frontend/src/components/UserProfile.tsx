import React, { useEffect, useState } from 'react';
import { useSearchParams } from "react-router-dom";
import { useProfile } from "../contexts/ProfileContext";
import { profileService } from "../services/profileService";
import { followService } from "../services/followService";
import { type ProfileDto } from "../types";
import EditProfileModal from "./EditProfileModal";
import MilestoneSection from "./MilestoneSection";
import PhotoSection from "./PhotoSection";
import UserListModal from "./UserListModal";

interface UserProfileProps {
  userId?: number | null;
}

const UserProfile: React.FC<UserProfileProps> = ({ userId }) => {
  const { profile: selfProfile, getProfile, updateProfileState } = useProfile();
  const [searchParams, setSearchParams] = useSearchParams();
  
  const [otherProfile, setOtherProfile] = useState<ProfileDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [activeModal, setActiveModal] = useState<'followers' | 'following' | null>(null);

  // Determine if we are viewing self or other
  const isSelf = !userId || userId === selfProfile?.id;
  const profile = isSelf ? selfProfile : otherProfile;

  useEffect(() => {
    if (isSelf) {
      getProfile();
    } else if (userId) {
      fetchOtherProfile(userId);
    }
  }, [userId, isSelf, getProfile]);

  const fetchOtherProfile = async (id: number) => {
    setLoading(true);
    try {
      const data = await profileService.getProfile(id);
      setOtherProfile(data);
    } catch (error) {
      console.error("Failed to fetch profile", error);
    } finally {
      setLoading(false);
    }
  };

  const handleFollowToggle = async () => {
    if (!profile || isSelf) return;

    try {
      if (profile.isFollowing) {
        await followService.unfollowUser(profile.id);
        setOtherProfile(prev => prev ? ({ 
          ...prev, 
          isFollowing: false, 
          followersCount: Math.max(0, prev.followersCount - 1) 
        }) : null);
      } else if (profile.isFollowRequested) {
        // Optional: Cancel request if API supports it, or just do nothing/unfollow
        await followService.unfollowUser(profile.id);
        setOtherProfile(prev => prev ? ({ ...prev, isFollowRequested: false }) : null);
      } else {
        await followService.followUser(profile.id);
        if (profile.isPrivate) {
          setOtherProfile(prev => prev ? ({ ...prev, isFollowRequested: true }) : null);
        } else {
          setOtherProfile(prev => prev ? ({ 
            ...prev, 
            isFollowing: true, 
            followersCount: prev.followersCount + 1 
          }) : null);
        }
      }
    } catch (error) {
      console.error("Failed to toggle follow status", error);
    }
  };

  // Tab state managed by URL params
  const activeTab = searchParams.get("tab") || "photos";

  const handleTabChange = (tab: string) => {
    setSearchParams(prev => {
        const newParams = new URLSearchParams(prev);
        newParams.set("tab", tab);
        return newParams;
    });
  };

  if (loading || !profile) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#b99547]"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        {/* Profile Header */}
        <div className="bg-white rounded-xl shadow-md overflow-hidden mb-8">
          <div className="relative h-32 w-full bg-gradient-to-r from-[#fadd9e] to-[#b99547]"></div>
          <div className="px-6 pb-6">
            <div className="relative flex items-start -mt-12 mb-4">
              <div className="relative">
                <img
                  src={profile.profileUrl || "src/assets/default-avatar.png"}
                  alt="Profile"
                  className="h-24 w-24 rounded-full border-4 border-white object-cover bg-white"
                />
              </div>
              <div className="ml-4 mt-12 flex-1">
                <h1 className="text-2xl font-bold text-gray-900 leading-tight">
                  {profile.displayName}
                </h1>
                <div className="flex items-center gap-4 mt-1 text-sm text-gray-600">
                  <button 
                    onClick={() => setActiveModal('followers')}
                    className="hover:text-[#b99547] cursor-pointer transition-colors focus:outline-none"
                  >
                    <span className="font-bold text-gray-900">{profile.followersCount}</span> Followers
                  </button>
                  <button 
                    onClick={() => setActiveModal('following')}
                    className="hover:text-[#b99547] cursor-pointer transition-colors focus:outline-none"
                  >
                    <span className="font-bold text-gray-900">{profile.followingCount}</span> Following
                  </button>
                </div>
                {profile.description && (
                  <p className="mt-4 text-gray-700 text-sm max-w-2xl">
                    {profile.description}
                  </p>
                )}
              </div>
              <div className="mt-12">
                {isSelf ? (
                  <button
                    onClick={() => setIsEditModalOpen(true)}
                    className="px-4 py-2 bg-white border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#b99547]"
                  >
                    Edit Profile
                  </button>
                ) : (
                  <button
                    onClick={handleFollowToggle}
                    className={`px-4 py-2 rounded-md text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#b99547] ${
                      profile.isFollowing || profile.isFollowRequested
                        ? "bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"
                        : "bg-[#b99547] text-white hover:bg-[#a0813d]"
                    }`}
                  >
                    {profile.isFollowing ? "Unfollow" : profile.isFollowRequested ? "Requested" : "Follow"}
                  </button>
                )}
              </div>
            </div>

            {/* Hobbies */}
            <div className="mt-4">
              <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wider mb-2">
                Hobbies & Interests
              </h3>
              <div className="flex flex-wrap gap-2">
                {profile.hobbies && profile.hobbies.length > 0 ? (
                  profile.hobbies.map((hobby, index) => (
                    <span
                      key={index}
                      className="inline-flex items-center px-3 py-0.5 rounded-full text-sm font-medium bg-[#f5e6c8] text-[#785c16]"
                    >
                      {hobby}
                    </span>
                  ))
                ) : (
                  <span className="text-gray-400 text-sm">No hobbies added yet.</span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="bg-white rounded-xl shadow-md overflow-hidden min-h-[500px]">
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex">
              <button
                onClick={() => handleTabChange("photos")}
                className={`w-1/2 py-4 px-1 text-center border-b-2 font-medium text-sm ${
                  activeTab === "photos"
                    ? "border-[#b99547] text-[#b99547]"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              >
                {isSelf ? "My Photos" : "Photos"}
              </button>
              <button
                onClick={() => handleTabChange("milestones")}
                className={`w-1/2 py-4 px-1 text-center border-b-2 font-medium text-sm ${
                  activeTab === "milestones"
                    ? "border-[#b99547] text-[#b99547]"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              >
                Milestones
              </button>
            </nav>
          </div>

          <div className="p-6">
            {activeTab === "photos" && (
                <PhotoSection 
                  photos={!isSelf ? (profile.photos || []) : undefined} 
                  milestones={!isSelf ? (profile.milestones || []) : undefined}
                  isReadOnly={!isSelf}
                />
            )}

            {activeTab === "milestones" && (
                <MilestoneSection 
                    milestones={!isSelf ? (profile.milestones || []) : undefined} 
                    photos={!isSelf ? (profile.photos || []) : undefined}
                    isReadOnly={!isSelf} 
                />
            )}
          </div>
        </div>
      </div>

      {isEditModalOpen && isSelf && (
        <EditProfileModal
          profile={profile}
          onClose={() => setIsEditModalOpen(false)}
          onSave={(updatedProfile) => {
            updateProfileState(updatedProfile);
          }}
        />
      )}

      {activeModal && (
        <UserListModal
          isOpen={!!activeModal}
          onClose={() => setActiveModal(null)}
          title={activeModal === 'followers' ? 'Followers' : 'Following'}
          type={activeModal}
          userId={profile.id}
        />
      )}
    </div>
  );
};

export default UserProfile;
