import React from 'react';
import { Navigate } from 'react-router-dom';

const MyRecognitions: React.FC = () => {
  return <Navigate to="/recognitions" replace />;
};

export default MyRecognitions;
