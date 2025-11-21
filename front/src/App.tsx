import './App.css';
import { ChakraProvider } from '@chakra-ui/react';
import { BrowserRouter } from 'react-router-dom';
import { Router } from './router/Router';
import { AuthProvider } from './components/context/AuthContext';

function App() {
  return (
    <ChakraProvider>
      <BrowserRouter basename='/app'>
        <AuthProvider>
          <Router />
        </AuthProvider>
      </BrowserRouter>
    </ChakraProvider>
  );
}

export default App;
