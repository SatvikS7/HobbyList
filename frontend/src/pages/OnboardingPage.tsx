import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from 'react-router-dom';
import { profileService } from "../services/profileService";



const Onboarding: React.FC = () => {
    const [step, setStep] = useState<"username" | "hobbies">("username");
    const [username, setUsername] = useState("");
    const [selectedHobbies, setSelectedHobbies] = useState<string[]>([]);
    const hobbies = Array.from({ length: 10 }, (_, i) => `dummy${i + 1}`);
    const navigate = useNavigate();



    // Shared animation variants
    const slideVariants = {
        enter: { x: "100%", opacity: 0 },
        center: { x: 0, opacity: 1 },
        exit: { x: "-100%", opacity: 0 },
    };

    const handleUsernameSubmit = async () => {
        if (!username.trim()) return alert("Please enter a username.");

        try {
            await profileService.updateProfile({ displayName: username.trim() });
            setStep("hobbies");
        } catch (err) {
            console.error("Error updating username:", err);
            alert("Failed to update username.");
        }
    };

    const toggleHobby = (hobby: string) => {
        setSelectedHobbies((prev) =>
            prev.includes(hobby)
            ? prev.filter((h) => h !== hobby)
            : [...prev, hobby]
        );
    };

    const handleHobbySubmit = async () => {
        try {
            await profileService.updateProfile({ hobbies: selectedHobbies });
            alert("Onboarding complete!");
            navigate('/home-page');
        } catch (err) {
            console.error("Error updating hobbies:", err);
            alert("Failed to update hobbies.");
        }
    };

    return (
    <div className="flex justify-center items-center min-h-screen bg-gradient-to-br from-[#fadd9e] to-[#daba76]">
        <div className="relative w-full max-w-md overflow-hidden rounded-2xl shadow-lg bg-white text-black">
        <AnimatePresence mode="wait">
            {step === "username" && (
            <motion.div
                key="username"
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.5 }}
                className="p-8 flex flex-col items-center text-center"
            >
                <h2 className="text-2xl font-bold mb-6 text-[#b99547]">
                Welcome! Choose your username
                </h2>

                <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter desired username"
                className="border border-gray-300 focus:border-[#b99547] focus:ring-[#b99547] outline-none p-3 rounded-md w-full mb-6 text-black transition"
                />

                <button
                onClick={handleUsernameSubmit}
                className="w-full bg-[#b99547] hover:bg-[#a07f36] text-black font-medium px-4 py-2 rounded-md transition"
                >
                Continue
                </button>
            </motion.div>
            )}

            {step === "hobbies" && (
            <motion.div
                key="hobbies"
                variants={slideVariants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: 0.5 }}
                className="p-8 flex flex-col items-center text-center"
            >
                <h2 className="text-2xl font-bold mb-6 text-[#b99547]">
                Select your hobbies
                </h2>

                <div className="grid grid-cols-2 gap-3 mb-8 w-full">
                {hobbies.map((hobby) => (
                    <button
                    key={hobby}
                    onClick={() => toggleHobby(hobby)}
                    className={`p-2 rounded-md border transition-colors ${
                        selectedHobbies.includes(hobby)
                        ? "bg-[#b99547] text-black border-[#a07f36]"
                        : "bg-gray-100 hover:bg-gray-200 border-gray-300 text-black"
                    }`}
                    >
                    {hobby}
                    </button>
                ))}
                </div>

                <button
                onClick={handleHobbySubmit}
                className="w-full bg-[#b99547] hover:bg-[#a07f36] text-black font-medium px-4 py-2 rounded-md transition"
                >
                Finish
                </button>
            </motion.div>
            )}
        </AnimatePresence>
        </div>
    </div>
    );

};

export default Onboarding;
