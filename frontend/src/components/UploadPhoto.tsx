import { useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import { useProfile } from "../contexts/ProfileContext";
import { usePhotoMilestone } from "../contexts/PhotoMilestoneContext";
import { photoService } from "../services/photoService";
import toast from "react-hot-toast";

function UploadPhoto() {
  const { token } = useAuth();
  const { profile, refreshProfile } = useProfile();
  const { milestoneMap, invalidatePhotos, invalidateMilestones} = usePhotoMilestone();
  
  const [file, setFile] = useState<File | null>(null);
  const [filename, setFilename] = useState("");
  const [description, setDescription] = useState("");
  const [topic, setTopic] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [selectedMilestoneIds, setSelectedMilestoneIds] = useState<number[]>([]);

  const allMilestones = Array.from(milestoneMap.values());

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
      setFilename(e.target.files[0].name);
    }
  };

  const toggleMilestoneSelection = (milestoneId: number) => {
    setSelectedMilestoneIds(prev => 
      prev.includes(milestoneId) 
        ? prev.filter(id => id !== milestoneId)
        : [...prev, milestoneId]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      toast.error("Please select a file before submitting.");
      return;
    }
    if (!token) {
      toast.error("Not authenticated");
      return;
    }

    try {
      setIsUploading(true);
      setUploadProgress(0);

      // 1. Get Presigned URL
      const uploadUrl = await photoService.getUploadUrl(filename, file.type);

      // 2. Upload to S3
      await photoService.uploadFileToS3(uploadUrl, file, (progress) => {
        setUploadProgress(progress);
      });

      // 3. Save Metadata
      const cleanUrl = uploadUrl.split("?")[0];

      if (selectedMilestoneIds.length > 0) {
        invalidateMilestones();
      }
      
      await photoService.savePhotoMetadata({
        topic,
        imageUrl: cleanUrl,
        filename,
        size: file.size,
        contentType: file.type,
        description,
        uploadDate: new Date().toISOString(),
        taggedMilestoneIds: selectedMilestoneIds,
      });

      // 4. Refresh Data
      invalidatePhotos();
      if (!profile?.hobbies.includes(topic)) {
        await refreshProfile();
      }

      toast.success("Upload successful!");
      
      // Reset form
      setFile(null);
      setFilename("");
      setDescription("");
      setTopic("");
      setUploadProgress(0);
      setSelectedMilestoneIds([]);
      
      // Reset file input manually
      const fileInput = document.getElementById("file-upload") as HTMLInputElement;
      if (fileInput) fileInput.value = "";

    } catch (error) {
      console.error("Upload failed:", error);
      toast.error("Upload failed. Please try again.");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-800 mb-6">Upload a Photo</h2>
      <p className="mb-6 text-sm text-gray-600">
        Share your hobby moments
      </p>
      
      <form className="space-y-6" onSubmit={handleSubmit}>
        <div className="rounded-md shadow-sm -space-y-px">
          {/* Topic */}
          <div className="mb-4">
            <label htmlFor="topic" className="block text-sm font-medium text-gray-700 mb-1">
              Topic / Hobby
            </label>
            <input
              id="topic"
              name="topic"
              type="text"
              required
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-[#b99547] focus:border-[#b99547] focus:z-10 sm:text-sm"
              placeholder="e.g. Hiking, Cooking"
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
            />
          </div>
          
          {/* Description */}
          <div className="mb-4">
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              id="description"
              name="description"
              rows={3}
              className="appearance-none rounded-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-[#b99547] focus:border-[#b99547] focus:z-10 sm:text-sm"
              placeholder="Tell us about this photo..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          {/* Milestone Tagging */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tag Milestones (Optional)
            </label>
            {allMilestones.length > 0 ? (
              <div className="max-h-60 overflow-y-auto border border-gray-300 rounded-md p-2 bg-white">
                {allMilestones.map((milestone) => (
                  <div 
                    key={milestone.id} 
                    className={`flex items-center p-2 rounded cursor-pointer hover:bg-gray-50 ${
                      selectedMilestoneIds.includes(milestone.id) ? "bg-[#fadd9e]/20" : ""
                    }`}
                    onClick={() => toggleMilestoneSelection(milestone.id)}
                  >
                    <div className={`w-4 h-4 rounded border flex items-center justify-center mr-3 ${
                      selectedMilestoneIds.includes(milestone.id) 
                        ? "bg-[#b99547] border-[#b99547]" 
                        : "border-gray-400"
                    }`}>
                      {selectedMilestoneIds.includes(milestone.id) && (
                        <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path>
                        </svg>
                      )}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-900">{milestone.task}</p>
                      <p className="text-xs text-gray-500">Due: {new Date(milestone.dueDate).toLocaleDateString()}</p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-gray-500 italic">No milestones available to tag.</p>
            )}
          </div>

          {/* File Upload */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Photo
            </label>
            <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md hover:border-[#b99547] transition-colors">
              <div className="space-y-1 text-center">
                <svg
                  className="mx-auto h-12 w-12 text-gray-400"
                  stroke="currentColor"
                  fill="none"
                  viewBox="0 0 48 48"
                  aria-hidden="true"
                >
                  <path
                    d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                    strokeWidth={2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                <div className="flex text-sm text-gray-600 justify-center">
                  <label
                    htmlFor="file-upload"
                    className="relative cursor-pointer bg-white rounded-md font-medium text-[#b99547] hover:text-[#a07f36] focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-[#b99547]"
                  >
                    <span>Upload a file</span>
                    <input
                      id="file-upload"
                      name="file-upload"
                      type="file"
                      className="sr-only"
                      onChange={handleFileChange}
                      accept="image/*"
                    />
                  </label>
                  <p className="pl-1">or drag and drop</p>
                </div>
                <p className="text-xs text-gray-500">PNG, JPG, GIF up to 10MB</p>
              </div>
            </div>
            {filename && (
              <p className="mt-2 text-sm text-gray-600">Selected: {filename}</p>
            )}
          </div>
        </div>

        {isUploading && (
          <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700">
            <div 
              className="bg-[#b99547] h-2.5 rounded-full transition-all duration-300" 
              style={{ width: `${uploadProgress}%` }}
            ></div>
            <p className="text-xs text-center mt-1 text-gray-500">{uploadProgress}% Uploaded</p>
          </div>
        )}

        <div>
          <button
            type="submit"
            disabled={isUploading}
            className={`group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white ${
              isUploading 
                ? "bg-[#d4b97b] cursor-not-allowed" 
                : "bg-[#b99547] hover:bg-[#a07f36] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#b99547]"
            }`}
          >
            {isUploading ? "Uploading..." : "Upload Photo"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default UploadPhoto;
