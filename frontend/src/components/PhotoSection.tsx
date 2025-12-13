import React, { useEffect, useState } from "react";
import { usePhotoMilestone } from "../contexts/PhotoMilestoneContext.tsx";
import PhotoCard from "./PhotoCard.tsx";
import { type PhotoDto, type MilestoneDto } from "../types/index.ts";

import UploadPhoto from "./UploadPhoto";

import { motion, AnimatePresence } from "framer-motion";

type PhotoSectionProps = {
  initialTag?: string;
  photos?: PhotoDto[];
  milestones?: MilestoneDto[];
  isReadOnly?: boolean;
};

const PhotoSection: React.FC<PhotoSectionProps> = ({ 
  initialTag = "All", 
  photos: propPhotos, 
  milestones: propMilestones,
  isReadOnly = false
}) => {
  const { photos: contextPhotos, getPhotos, milestones: contextMilestones } = usePhotoMilestone();
  
  // Use props if available, otherwise fallback to context
  const photos = propPhotos || contextPhotos;
  const milestones = propMilestones || contextMilestones || [];

  const [filteredPhotos, setFilteredPhotos] = useState<PhotoDto[]>([]);
  const [tags, setTags] = useState<string[]>([]);
  const [selectedTag, setSelectedTag] = useState<string>(initialTag);
  const [selectedPhoto, setSelectedPhoto] = useState<PhotoDto | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  // Fetch photos from cache or backend (only if using context)
  const fetchPhotos = async () => {
    if (propPhotos) {
        setFilteredPhotos(propPhotos);
        const uniqueTags = Array.from(new Set(propPhotos.map((p) => p.topic)));
        setTags(uniqueTags);
        return;
    }

    try {
      const data = await getPhotos();
      if (data) {
        setFilteredPhotos(data);
        // extract unique tags for filter
        const uniqueTags = Array.from(new Set(data.map((p) => p.topic)));
        setTags(uniqueTags);
      }
    } catch (err) {
      console.error("Failed to fetch photos", err);
    }
  };

  useEffect(() => {
    fetchPhotos();
  }, [propPhotos]); // Re-run if propPhotos changes

  // Filter photos by tag
  useEffect(() => {
    if (selectedTag === "All") {
      setFilteredPhotos(photos ?? []);
    } else {
      setFilteredPhotos((photos ?? []).filter((p) => p.topic === selectedTag));
    }
  }, [selectedTag, photos]);

  return (
    <div className="max-w-4xl mx-auto p-6">
      {!isReadOnly && (
        <div className="mb-8">
           <button
            onClick={() => setIsUploading(prev => !prev)}
            className="flex items-center gap-2 text-[#b99547] font-semibold hover:text-[#a07f36] transition-colors mb-4"
          >
            <svg 
              xmlns="http://www.w3.org/2000/svg" 
              className={`h-5 w-5 transition-transform ${isUploading ? "rotate-90" : ""}`}
              viewBox="0 0 20 20" 
              fill="currentColor"
            >
              <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
            </svg>
            Upload New Photo
          </button>
          
          <AnimatePresence>
            {isUploading && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: "auto", opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="overflow-hidden"
              >
                <div className="bg-white rounded-xl shadow-md p-6">
                  <UploadPhoto />
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}
      {/* Filter */}
      <div className="mb-6 flex items-center gap-2">
        <label htmlFor="tagFilter" className="font-medium text-gray-800">
          Filter by tag:
        </label>
        <select
          id="tagFilter"
          className="border border-gray-300 rounded px-2 py-1 text-black"
          value={selectedTag}
          onChange={(e) => setSelectedTag(e.target.value)}
        >
          <option value="All">All</option>
          {tags.map((tag) => (
            <option key={tag} value={tag}>{tag}</option>
          ))}
        </select>
      </div>

      {/* Photo grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
        {filteredPhotos.map((photo, idx) => (
          <div
            key={idx}
            className="rounded-xl shadow-md overflow-hidden border border-gray-200 cursor-pointer"
            onClick={() => setSelectedPhoto(photo)}
          >
            <img
              src={photo.imageUrl}
              alt={photo.topic}
              className="w-full h-48 object-cover"
            />
          </div>
        ))}
      </div>

      {/* Photo modal */}
      {selectedPhoto && (
        <PhotoCard
          imageUrl={selectedPhoto.imageUrl}
          topic={selectedPhoto.topic}
          description={selectedPhoto.description}
          uploadDate={selectedPhoto.uploadDate}
          taggedMilestoneIds={selectedPhoto.taggedMilestoneIds}
          onClose={() => setSelectedPhoto(null)}
          milestones={milestones}
        />
      )}
    </div>
  );
};

export default PhotoSection;
