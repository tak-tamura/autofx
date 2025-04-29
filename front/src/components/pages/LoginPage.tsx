// src/components/LoginForm.tsx
import React, { useState, FormEvent } from 'react';
import {
  Box,
  Heading,
  FormControl,
  FormLabel,
  Input,
  FormErrorMessage,
  Button,
  Alert,
  AlertIcon,
  Spinner,
  useColorModeValue,
} from '@chakra-ui/react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      await login(username, password);
      navigate("/home");
    } catch (err: any) {
      setError(err.message || 'ログインに失敗しました');
    } finally {
      setIsLoading(false);
    }
  };

  const bg = useColorModeValue('white', 'gray.700');

  return (
    <Box maxW="md" mx="auto" mt="20" p="8" bg={bg} boxShadow="lg" borderRadius="lg">
      <Heading mb="6" textAlign="center" size="lg">
        ログイン
      </Heading>

      {error && (
        <Alert status="error" mb="4" borderRadius="md">
          <AlertIcon />
          {error}
        </Alert>
      )}

      <form onSubmit={handleSubmit}>
        <FormControl id="username" mb="4" isRequired isInvalid={!!error}>
          <FormLabel>ユーザー名</FormLabel>
          <Input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="ユーザー名を入力"
          />
          <FormErrorMessage>{error}</FormErrorMessage>
        </FormControl>

        <FormControl id="password" mb="6" isRequired isInvalid={!!error}>
          <FormLabel>パスワード</FormLabel>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="パスワードを入力"
          />
          <FormErrorMessage>{error}</FormErrorMessage>
        </FormControl>

        <Button
          type="submit"
          colorScheme="teal"
          width="full"
          isLoading={isLoading}
          loadingText="ログイン中"
        >
          {!isLoading && 'ログイン'}
        </Button>
      </form>
    </Box>
  );
};