import { useState } from "react";
import { useNavigate } from 'react-router-dom';

const API_BASE = import.meta.env.VITE_BACKEND_BASE;

function PasswordReset() {
    const [email, setEmail] = useState("");
    const [status, setStatus] = useState("");
    const navigate = useNavigate();

    const handleEmailSubmit = async(e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch(`${API_BASE}/auth/request-password-reset`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ email }),
            });

            if (!response.ok) {
                throw new Error("Failed to send password reset email");
            }

            setStatus("Password reset email sent successfully!");
            setTimeout(() => {
                navigate("/password-reset");
            }, 3000);
        } catch (error) {
            console.error("Error sending password reset email:", error);
            alert("Password reset email failed. Please try again.");
        }
    };

    return (
        <div className="h-screen flex justify-center items-center bg-white">
            <div className="flex flex-col items-center justify-center border-2 border-black rounded-[5%] bg-gradient-to-br from-[#fadd9e] to-[#daba76] p-8 w-full max-w-md">
                <h1 className="text-3xl font-bold text-[#000000] mb-6">Email</h1>
                <form onSubmit={handleEmailSubmit} className="flex flex-col gap-4 w-full">
                    <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="Enter your email"
                        required
                        className="mt-1 p-2 border border-white rounded bg-white text-black focus:outline-none focus:ring-2 focus:ring-[#b99547]"
                    />
                    <button type="submit" className="mt-4 px-4 py-2 rounded-lg bg-[#c77903] text-white font-semibold hover:bg-[#b36b02] transition-colors duration-200">Send Password Reset Email</button>
                </form>
                {status && <p className="status-text">{status}</p>}
            </div>
        </div>
        
    );
}

export default PasswordReset;

         