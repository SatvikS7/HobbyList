import { Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import './App.css'
import SignupPage from './pages/SignupPage';
import LoginPage from './pages/LoginPage'
import LandingPage from './pages/LandingPage'
import VerificationPage from './pages/VerificationPage';
import PasswordResetForm from './pages/PasswordResetForm';
import PasswordReset from './pages/PasswordReset';
import HomePage from './pages/HomePage';
import Header from './components/Header';
import ProfilePage from './pages/ProfilePage';
import DiscoveryPage from './pages/DiscoveryPage';
import Onboarding from './pages/OnboardingPage';
import MilestoneTreePage from './pages/MilestoneTreePage';

function App() {
  return (
    <div className='body'>
      <Toaster position="top-center" reverseOrder={false} />
      <Header />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/verification" element={<VerificationPage />} />
        <Route path="/password-reset" element={<PasswordReset />} />
        <Route path="/reset-password" element={<PasswordResetForm />} />
        <Route path="/home-page" element={<HomePage />} />
        <Route path="/profile-page" element={<ProfilePage />} />
        <Route path="/discovery-page" element={<DiscoveryPage />} />
        <Route path="/onboarding-page" element={<Onboarding />} />
        <Route path="/milestone-tree" element={<MilestoneTreePage />} />
      </Routes>
    </div>
  )
}

export default App
