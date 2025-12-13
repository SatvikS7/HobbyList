import { useState } from "react";
import { usePhotoMilestone } from "../contexts/PhotoMilestoneContext";
import { milestoneService } from "../services/milestoneService";
import MilestoneCard from "./MilestoneCard";
import MilestoneItem from "./MilestoneItem";
import toast from "react-hot-toast";
import { motion, AnimatePresence } from "framer-motion";
import { type MilestoneDto, type PhotoDto } from "../types";

interface MilestoneSectionProps {
  milestones?: MilestoneDto[];
  photos?: PhotoDto[];
  isReadOnly?: boolean;
}

function MilestoneSection({ 
  milestones: propMilestones, 
  photos: propPhotos, 
  isReadOnly = false 
}: MilestoneSectionProps) {
  const { 
    milestones: contextMilestones, 
    refreshMilestones, 
    photos: contextPhotos, 
    invalidatePhotos
  } = usePhotoMilestone();
  
  // Use props if available, otherwise fallback to context
  const milestones = propMilestones || contextMilestones;
  const photos = propPhotos || contextPhotos;

  const [newTask, setNewTask] = useState("");
  const [newDueDate, setNewDueDate] = useState("");
  const [newDueTime, setNewDueTime] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<number[]>([]);

  // Filter states
  const [filterStatus, setFilterStatus] = useState<"ALL" | "COMPLETED" | "PENDING">("ALL");
  const [selectedMilestone, setSelectedMilestone] = useState<MilestoneDto | null>(null);

  const togglePhotoSelection = (photoId: number) => {
    setSelectedPhotoIds(prev => 
      prev.includes(photoId) 
        ? prev.filter(id => id !== photoId)
        : [...prev, photoId]
    );
  };

  const handleCreateMilestone = async () => {
    if (!newTask.trim() || !newDueDate || !newDueTime) {
      toast.error("Please fill out all fields.");
      return;
    }

    try {
      setIsSubmitting(true);

      // Construct ISO string properly
      const dateTimeString = `${newDueDate}T${newDueTime}:00`;
      const localDate = new Date(dateTimeString);
      const isoString = localDate.toISOString();

      if (selectedPhotoIds.length === 0) {
        invalidatePhotos();
      }

      await milestoneService.createMilestone({
        task: newTask,
        dueDate: isoString,
        completed: false,
        taggedPhotoIds: selectedPhotoIds,
      });

      toast.success("Milestone created!");
      setNewTask("");
      setNewDueDate("");
      setNewDueTime("");
      setSelectedPhotoIds([]);
      setIsCreating(false); // Close the toggle
      await refreshMilestones();
    } catch (error) {
      console.error("Error creating milestone:", error);
      toast.error("Failed to create milestone.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteMilestone = async (id: number) => {
    if (isReadOnly) return;
    if (!window.confirm("Are you sure you want to delete this milestone?")) return;

    try {
      await milestoneService.deleteMilestone(id);
      toast.success("Milestone deleted");
      await refreshMilestones();
    } catch (error) {
      console.error("Error deleting milestone:", error);
      toast.error("Failed to delete milestone");
    }
  };

  const filteredMilestones = (milestones ? milestones : [])
    .filter((m) => m.parentId === null) // Only show top-level milestones
    .filter((m) => {
      if (filterStatus === "ALL") return true;
      if (filterStatus === "COMPLETED") return m.completed;
      if (filterStatus === "PENDING") return !m.completed;
      return true;
    });

  return (
    <div className="max-w-4xl mx-auto p-6">
      {!isReadOnly && (
        <div className="mb-8">
          <button
            onClick={() => setIsCreating(prev => !prev)}
            className="flex items-center gap-2 text-[#b99547] font-semibold hover:text-[#a07f36] transition-colors mb-4"
          >
            <svg 
              xmlns="http://www.w3.org/2000/svg" 
              className={`h-5 w-5 transition-transform ${isCreating ? "rotate-90" : ""}`}
              viewBox="0 0 20 20" 
              fill="currentColor"
            >
              <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
            </svg>
            Add New Milestone
          </button>
          
          <AnimatePresence>
            {isCreating && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: "auto", opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="overflow-hidden"
              >
                <div className="bg-white rounded-xl shadow-md p-6">
                  <h2 className="text-2xl font-bold text-gray-800 mb-6">Create New Milestone</h2>
                  <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-end">
                    <div className="md:col-span-5">
                      <label className="block text-sm font-medium text-gray-700 mb-1">Task Name</label>
                      <input
                        type="text"
                        placeholder="e.g. Complete 5 hikes"
                        className="w-full p-2 border border-gray-300 rounded-md focus:ring-[#b99547] focus:border-[#b99547] text-gray-800"
                        value={newTask}
                        onChange={(e) => setNewTask(e.target.value)}
                      />
                    </div>
                    <div className="md:col-span-3">
                      <label className="block text-sm font-medium text-gray-700 mb-1">Due Date</label>
                      <input
                        type="date"
                        className="w-full p-2 border border-gray-300 rounded-md focus:ring-[#b99547] focus:border-[#b99547] text-gray-800"
                        value={newDueDate}
                        min={new Date().toISOString().split("T")[0]}
                        onChange={(e) => setNewDueDate(e.target.value)}
                      />
                    </div>
                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">Time</label>
                      <input
                        type="time"
                        className="w-full p-2 border border-gray-300 rounded-md focus:ring-[#b99547] focus:border-[#b99547] text-gray-800"
                        value={newDueTime}
                        onChange={(e) => setNewDueTime(e.target.value)}
                      />
                    </div>
                    <div className="md:col-span-2">
                      <button
                        onClick={handleCreateMilestone}
                        disabled={isSubmitting}
                        className="w-full bg-[#b99547] text-white p-2 rounded-md hover:bg-[#a07f36] transition-colors disabled:bg-[#d4b97b]"
                      >
                        {isSubmitting ? "Adding..." : "Add"}
                      </button>
                    </div>
                  </div>

                  {/* Photo Selection */}
                  <div className="mt-6">
                    <h3 className="text-sm font-medium text-gray-700 mb-2">Tag Photos (Optional)</h3>
                    {photos && photos.length > 0 ? (
                      <div className="flex flex-wrap gap-2 max-h-40 overflow-y-auto p-2 border border-gray-200 rounded-md">
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
                              className="w-16 h-16 object-cover rounded-md"
                            />
                            {selectedPhotoIds.includes(photo.id) && (
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
                      <p className="text-sm text-gray-500 italic">No photos available to tag.</p>
                    )}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}

      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Your Milestones</h2>
        <div className="flex space-x-2">
          {(["ALL", "PENDING", "COMPLETED"] as const).map((status) => (
            <button
              key={status}
              onClick={() => setFilterStatus(status)}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                filterStatus === status
                  ? "bg-[#b99547] text-white"
                  : "bg-gray-200 text-gray-700 hover:bg-gray-300"
              }`}
            >
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-4">
        <AnimatePresence>
          {filteredMilestones.length > 0 ? (
            filteredMilestones.map((milestone) => (
              <motion.div
                key={milestone.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                layout
              >
                <MilestoneItem
                  milestone={milestone}
                  onDelete={handleDeleteMilestone}
                  onClick={setSelectedMilestone}
                  isReadOnly={isReadOnly}
                />
              </motion.div>
            ))
          ) : (
            <p className="text-center text-gray-500 py-8">No milestones found.</p>
          )}
        </AnimatePresence>
      </div>

      {/* Milestone Details Modal */}
      {selectedMilestone && (
        <MilestoneCard
          milestone={selectedMilestone}
          onClose={() => setSelectedMilestone(null)}
          onDelete={() => handleDeleteMilestone(selectedMilestone.id)}
          isReadOnly={isReadOnly}
          photos={photos || undefined}
        />
      )}
    </div>
  );
}

export default MilestoneSection;
