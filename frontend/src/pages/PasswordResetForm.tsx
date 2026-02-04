import { useState } from "react";

const API_BASE = import.meta.env.VITE_BACKEND_BASE;

function PasswordResetForm() {
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const handlePasswordReset = async(e: React.FormEvent) => {
        e.preventDefault();

        const params = new URLSearchParams(window.location.search);
        const token = params.get("token");
        if (password !== confirmPassword) {
            alert("Passwords do not match");
            return;
        }

        try {
            console.log(confirmPassword)
            const response = await fetch(`${API_BASE}/auth/reset-password?token=${token}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ password }),
            });

            if (!response.ok) {
                throw new Error("Failed to reset password");
            }

            alert("Password reset successfully!");
        } catch (error) {
            console.error("Error resetting password:", error);
            alert("Password reset failed. Please try again.");
        }
    }

    return (
        <div className="h-screen flex justify-center items-center bg-white">
            <div className="flex flex-col items-center justify-center border-2 border-black rounded-[5%] bg-gradient-to-br from-[#fadd9e] to-[#daba76] p-8 w-full max-w-md">
                <h1 className="text-3xl font-bold text-[#000000] mb-6">Password Reset</h1>
                <form onSubmit={handlePasswordReset} className="flex flex-col gap-4 w-full">
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Enter your new password"
                        required
                        className="mt-1 p-2 border border-white rounded bg-white text-black focus:outline-none focus:ring-2 focus:ring-[#b99547]"
                    />
                    <input
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Confirm your new password"
                        required
                        className="mt-1 p-2 border border-white rounded bg-white text-black focus:outline-none focus:ring-2 focus:ring-[#b99547]"
                    />
                    <button type="submit" className="mt-4 px-4 py-2 rounded-lg bg-[#c77903] text-white font-semibold hover:bg-[#b36b02] transition-colors duration-200">Reset Password</button>
                </form>
            </div>
        </div>
    );
}


export default PasswordResetForm;