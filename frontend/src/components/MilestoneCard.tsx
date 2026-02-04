import React, { useState, useEffect } from "react";
import { usePhotoMilestone } from "../contexts/PhotoMilestoneContext";
import { useProfile } from "../contexts/ProfileContext";
//import PhotoCard from "./PhotoCard";
import { milestoneService } from "../services/milestoneService";
import toast from "react-hot-toast";
import { type MilestoneDto, type PhotoDto } from "../types";

interface MilestoneCardProps {
  milestone: MilestoneDto;
  onClose: () => void;
  onDelete?: () => void;
  isReadOnly?: boolean;
  photos?: PhotoDto[];
}

const MilestoneCard: React.FC<MilestoneCardProps> = ({ milestone, onClose, onDelete, isReadOnly = false, photos }) => {
  const { milestoneMap, photoMap, refreshMilestones, invalidatePhotos } = usePhotoMilestone();
  const { profile } = useProfile();
  const [selectedPhotoId, setSelectedPhotoId] = useState<number | null>(null);

  // Edit Mode State
  const [isEditing, setIsEditing] = useState(false);
  const [editTask, setEditTask] = useState(milestone.task);
  const [editDate, setEditDate] = useState("");
  const [editTime, setEditTime] = useState("");
  const [editHobbyTag, setEditHobbyTag] = useState(milestone.hobbyTag || "");
  const [editTaggedPhotoIds, setEditTaggedPhotoIds] = useState<number[]>(milestone.taggedPhotoIds);
  const [isSaving, setIsSaving] = useState(false);

  // Subtask creation state
  const [isAddingSubtask, setIsAddingSubtask] = useState(false);
  const [subtaskTask, setSubtaskTask] = useState("");
  const [subtaskDate, setSubtaskDate] = useState("");
  const [subtaskTime, setSubtaskTime] = useState("");
  const [isCreatingSub, setIsCreatingSub] = useState(false);
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<number[]>([]);

  useEffect(() => {
    if (milestone) {
      const dateObj = new Date(milestone.dueDate);
      setEditTask(milestone.task);
      setEditDate(dateObj.toISOString().split("T")[0]);
      setEditTime(dateObj.toTimeString().slice(0, 5));
      setEditHobbyTag(milestone.hobbyTag || "");
      setEditTaggedPhotoIds(milestone.taggedPhotoIds);
    }
  }, [milestone]);

  const taggedPhotos = milestone.taggedPhotoIds
    .map((id) => {
        if (photos) {
            return photos.find(p => p.id === id);
        }
        return photoMap.get(id);
    })
    .filter(Boolean);

  const togglePhotoSelection = (photoId: number) => {
    setSelectedPhotoIds(prev => 
      prev.includes(photoId) 
        ? prev.filter(id => id !== photoId)
        : [...prev, photoId]
    );
  };

  const toggleEditPhotoSelection = (photoId: number) => {
    setEditTaggedPhotoIds(prev => 
      prev.includes(photoId) 
        ? prev.filter(id => id !== photoId)
        : [...prev, photoId]
    );
  };

  const handleSaveEdit = async () => {
    if (!editTask.trim() || !editDate || !editTime) {
      toast.error("Please fill out all required fields.");
      return;
    }

    try {
      setIsSaving(true);
      const dateTimeString = `${editDate}T${editTime}:00`;
      const localDate = new Date(dateTimeString);
      const isoString = localDate.toISOString();
      
      const updatedMilestone: MilestoneDto = {
        ...milestone,
        task: editTask,
        dueDate: isoString,
        completed: milestone.completed, // Keep original status
        hobbyTag: editHobbyTag.trim() || null,
        taggedPhotoIds: editTaggedPhotoIds,
      };
      invalidatePhotos();
      await milestoneService.editMilestone(updatedMilestone);

      await refreshMilestones();

      toast.success("Milestone updated!");
      onClose();
    } catch (error) {
      console.error("Error updating milestone:", error);
      toast.error("Failed to update milestone.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleAddSubtask = async () => {
    if (!subtaskTask.trim() || !subtaskDate || !subtaskTime) {
      toast.error("Please fill out all fields for the subtask.");
      return;
    }

    try {
      setIsCreatingSub(true);
      const dateTimeString = `${subtaskDate}T${subtaskTime}:00`;
      const localDate = new Date(dateTimeString);
      const isoString = localDate.toISOString();

      if (selectedPhotoIds.length != 0) {
        invalidatePhotos();
      }

      await milestoneService.createMilestone({
        task: subtaskTask,
        dueDate: isoString,
        completed: false,
        parentId: milestone.id,
        taggedPhotoIds: selectedPhotoIds,
      });

      toast.success("Subtask created!");
      setSubtaskTask("");
      setSubtaskDate("");
      setSubtaskTime("");
      setSelectedPhotoIds([]);
      setIsAddingSubtask(false);
      await refreshMilestones();
    } catch (error) {
      console.error("Error creating subtask:", error);
      toast.error("Failed to create subtask.");
    } finally {
      setIsCreatingSub(false);
    }
  };

  const renderSubMilestones = (subs: MilestoneDto[]) => {
    if (!subs || subs.length === 0) return null;

    return (
      <ul className="pl-4 border-l border-gray-300 space-y-2 mt-2">
        {subs.map((sub) => {
          const subPercentage = (milestoneMap.get(sub.id)?.completionRate ?? 0) * 100;
          return (
            <li key={sub.id}>
              <div className="flex justify-between items-center">
                <span className="text-gray-800 font-medium">{sub.task}</span>
                <span className="text-gray-500 text-sm">{sub.hobbyTag}</span>
              </div>
              <div className="text-gray-600 text-sm">
                Due: {new Date(sub.dueDate).toLocaleDateString()} |{" "}
                {sub.completed ? "Completed" : "Pending"}
              </div>
              {/* Sub-milestone Progress Bar */}
               <div className="w-full max-w-[150px] mt-1">
                <div className="w-full bg-gray-200 rounded-full h-1">
                  <div
                    className="bg-[#b99547] h-1 rounded-full transition-all duration-300"
                    style={{ width: `${subPercentage}%` }}
                  />
                </div>
              </div>
              {renderSubMilestones(sub.subMilestones)}
            </li>
          );
        })}
      </ul>
    );
  };
  //const selectedPhoto = selectedPhotoId ? photoMap.get(selectedPhotoId) : null;
  const currentPercentage = (milestoneMap.get(milestone.id)?.completionRate ?? 0) * 100;

  return (
    <>
      <div
        className="fixed inset-0 w-full h-full backdrop-blur-lg bg-black/20 flex justify-center items-center z-50"
        onClick={(e) => e.target === e.currentTarget && onClose()}
      >
        <div
          className="bg-white w-[90%] md:w-[70%] max-h-[90vh] rounded-lg overflow-auto p-6 shadow-xl relative"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Close */}
          <button
            className="absolute top-3 right-3 text-gray-700 hover:text-black px-2 py-1"
            onClick={onClose}
          >
            X
          </button>
          
          {/* Actions */}
          <div className="absolute top-3 right-12 flex gap-2">
            {!isEditing && !isReadOnly && (
              <>
                <button
                  className="text-[#b99547] hover:text-[#a07f36] px-2 py-1"
                  onClick={() => setIsEditing(true)}
                >
                  Edit
                </button>
                {onDelete && (
                  <button
                    className="text-red-500 hover:text-red-700 px-2 py-1"
                    onClick={() => {
                      if (window.confirm("Are you sure you want to delete this milestone?")) {
                        onDelete();
                        onClose();
                      }
                    }}
                  >
                    Delete
                  </button>
                )}
              </>
            )}
          </div>

          {isEditing ? (
            <div className="space-y-4 mt-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Edit Milestone</h2>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Task Name</label>
                <input
                  type="text"
                  className="w-full p-2 border border-gray-300 rounded-md text-black"
                  value={editTask}
                  onChange={(e) => setEditTask(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Due Date</label>
                  <input
                    type="date"
                    className="w-full p-2 border border-gray-300 rounded-md text-black"
                    value={editDate}
                    min={(() => {
                      let minDateStr = new Date().toISOString().split("T")[0];
                      if (milestone.subMilestones && milestone.subMilestones.length > 0) {
                        const latestChildTimestamp = milestone.subMilestones.reduce((max, child) => {
                          const t = new Date(child.dueDate).getTime();
                          return t > max ? t : max;
                        }, 0);
                        if (latestChildTimestamp > 0) {
                          const latestChildDateStr = new Date(latestChildTimestamp).toISOString().split("T")[0];
                          if (latestChildDateStr > minDateStr) {
                            minDateStr = latestChildDateStr;
                          }
                        }
                      }
                      return minDateStr;
                    })()}
                    max={
                      milestone.parentId && milestoneMap.get(milestone.parentId)?.dueDate
                        ? new Date(milestoneMap.get(milestone.parentId)!.dueDate).toISOString().split("T")[0]
                        : undefined
                    }
                    onChange={(e) => setEditDate(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Time</label>
                  <input
                    type="time"
                    className="w-full p-2 border border-gray-300 rounded-md text-black"
                    value={editTime}
                    onChange={(e) => setEditTime(e.target.value)}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Hobby Tag</label>
                <input
                  type="text"
                  list="hobby-suggestions"
                  className="w-full p-2 border border-gray-300 rounded-md text-black"
                  value={editHobbyTag}
                  onChange={(e) => setEditHobbyTag(e.target.value)}
                  placeholder="e.g. Hiking"
                />
                <datalist id="hobby-suggestions">
                  {profile?.hobbies.map((hobby) => (
                    <option key={hobby} value={hobby} />
                  ))}
                </datalist>
              </div>


              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Tagged Photos</label>
                {photos && photos.length > 0 ? (
                  <div className="flex flex-wrap gap-2 max-h-40 overflow-y-auto p-2 border border-gray-200 rounded-md">
                    {photos.map((photo) => (
                      <div 
                        key={photo.id} 
                        className={`relative cursor-pointer group ${
                          editTaggedPhotoIds.includes(photo.id) ? "ring-2 ring-[#b99547]" : ""
                        }`}
                        onClick={() => toggleEditPhotoSelection(photo.id)}
                      >
                        <img
                          src={photo.imageUrl}
                          alt={photo.topic}
                          className="w-16 h-16 object-cover rounded-md"
                        />
                        {editTaggedPhotoIds.includes(photo.id) && (
                          <div className="absolute inset-0 bg-[#b99547]/20 rounded-md flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-white drop-shadow-md" viewBox="0 0 20 20" fill="currentColor">
                              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                            </svg>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 italic">No photos available.</p>
                )}
              </div>

              <div className="flex justify-end gap-3 mt-4">
                <button
                  onClick={() => setIsEditing(false)}
                  className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
                  disabled={isSaving}
                >
                  Cancel
                </button>
                <button
                  onClick={handleSaveEdit}
                  disabled={isSaving}
                  className="px-4 py-2 bg-[#b99547] text-white rounded-md hover:bg-[#a07f36] disabled:bg-[#d4b97b]"
                >
                  {isSaving ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </div>
          ) : (
            <>
              {/* Milestone Info */}
              <h2 className="text-2xl font-semibold text-gray-900 mb-2">{milestone.task}</h2>
              <p className="text-gray-600 mb-1">Due: {new Date(milestone.dueDate).toLocaleDateString()}</p>
              <p className="text-gray-600 mb-2">Status: {milestone.completed ? "Completed" : "Pending"}</p>
              
              {/* Progress Bar */}
              <div className="mb-4">
                <div className="flex justify-between text-sm text-gray-600 mb-1">
                  <span>Progress</span>
                  <span>{Math.round(currentPercentage)}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2.5">
                  <div
                    className="bg-[#b99547] h-2.5 rounded-full transition-all duration-300"
                    style={{ width: `${currentPercentage}%` }}
                  />
                </div>
              </div>

              {milestone.hobbyTag && <p className="text-gray-600 mb-2">Hobby: {milestone.hobbyTag}</p>}

              {/* Tagged Photos */}
              {taggedPhotos.length > 0 && (
                <div className="mt-4">
                  <h3 className="font-semibold text-gray-700 mb-2">Tagged Photos</h3>
                  <div className="flex flex-wrap gap-2">
                    {taggedPhotos.map((p) => (
                      <img
                        key={p!.id}
                        src={p!.imageUrl}
                        alt={p!.topic}
                        className="w-20 h-20 object-cover rounded border border-gray-300"
                        onClick={() => setSelectedPhotoId(p!.id)}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* Sub-milestones Section */}
              <div className="mt-6">
                <div className="flex justify-between items-center mb-2">
                  <h3 className="font-semibold text-gray-700">Sub-milestones</h3>
                  {!isReadOnly && (
                    <button
                      onClick={() => setIsAddingSubtask(!isAddingSubtask)}
                      className="text-sm text-[#b99547] hover:text-[#a07f36] font-medium"
                    >
                      {isAddingSubtask ? "Cancel" : "+ Add Subtask"}
                    </button>
                  )}
                </div>

                {isAddingSubtask && (
                  <div className="bg-gray-50 p-4 rounded-md mb-4 border border-gray-200">
                    <div className="grid grid-cols-1 gap-3">
                      <div>
                        <label className="block text-xs font-medium text-gray-700 mb-1">Task Name</label>
                        <input
                          type="text"
                          className="w-full p-2 border border-gray-300 rounded-md text-sm text-gray-900"
                          value={subtaskTask}
                          onChange={(e) => setSubtaskTask(e.target.value)}
                          placeholder="Subtask name"
                        />
                      </div>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="block text-xs font-medium text-gray-700 mb-1">Date</label>
                          <input
                            type="date"
                            className="w-full p-2 border border-gray-300 rounded-md text-sm text-gray-900"
                            value={subtaskDate}
                            min={new Date().toISOString().split("T")[0]}
                            max={new Date(milestone.dueDate).toISOString().split("T")[0]}
                            onChange={(e) => setSubtaskDate(e.target.value)}
                          />
                        </div>
                        <div>
                          <label className="block text-xs font-medium text-gray-700 mb-1">Time</label>
                          <input
                            type="time"
                            className="w-full p-2 border border-gray-300 rounded-md text-sm text-gray-900"
                            value={subtaskTime}
                            onChange={(e) => setSubtaskTime(e.target.value)}
                          />
                        </div>
                      </div>
                      <div className="flex justify-end mt-2">
                        <button
                          onClick={handleAddSubtask}
                          disabled={isCreatingSub}
                          className="bg-[#b99547] text-white px-3 py-1.5 rounded text-sm hover:bg-[#a07f36] disabled:bg-gray-400"
                        >
                          {isCreatingSub ? "Adding..." : "Add Subtask"}
                        </button>
                      </div>
                    </div>

                    {/* Photo Selection for Subtask */}
                    <div className="mt-4 border-t border-gray-200 pt-3">
                      <h4 className="text-xs font-medium text-gray-700 mb-2">Tag Photos (Optional)</h4>
                      {photos && photos.length > 0 ? (
                        <div className="flex flex-wrap gap-2 max-h-32 overflow-y-auto p-2 border border-gray-200 rounded-md bg-white">
                          {photos.map((photo) => (
                            <div 
                              key={photo.id} 
                              className={`relative cursor-pointer group ${
                                selectedPhotoIds.includes(photo.id) ? "ring-2 ring-[#b99547]" : ""
                              }`}
                              onClick={() => togglePhotoSelection(photo.id)}
                            >
                              <img
                                src={photo.imageUrl}
                                alt={photo.topic}
                                className="w-12 h-12 object-cover rounded-md"
                                />
                              {selectedPhotoIds.includes(photo.id) && (
                                <div className="absolute inset-0 bg-[#b99547]/20 rounded-md flex items-center justify-center">
                                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-white drop-shadow-md" viewBox="0 0 20 20" fill="currentColor">
                                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                                  </svg>
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-xs text-gray-500 italic">No photos available.</p>
                      )}
                    </div>  
                  </div>
                )}

                {milestone.subMilestones.length > 0 ? (
                  renderSubMilestones(milestone.subMilestones)
                ) : (
                  !isAddingSubtask && <p className="text-gray-500 text-sm italic">No sub-milestones yet.</p>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default MilestoneCard;
