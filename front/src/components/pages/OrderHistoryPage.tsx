import React, { useState } from "react";
import {
  Box,
  Container,
  Heading,
  Stack,
  HStack,
  VStack,
  FormControl,
  FormLabel,
  Input,
  NumberInput,
  NumberInputField,
  Button,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  Text,
  Tag,
  Spinner,
  Alert,
  AlertIcon,
  Stat,
  StatLabel,
  StatNumber,
  StatHelpText,
  Divider,
} from "@chakra-ui/react";
import { useOrderHistory } from "../../hooks/useOrderHistory";

export const OrderHistoryPage: React.FC = () => {
  // 検索条件（シンプルに state 直結）
  const [startDate, setStartDate] = useState<string>("2024-01-01");
  const [endDate, setEndDate] = useState<string>("2024-01-31");
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(10);

  const { data, loading, error } = useOrderHistory({
    page,
    size,
    startDate,
    endDate,
  });

  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const handlePrevPage = () => {
    setPage((p) => Math.max(p - 1, 0));
  };

  const handleNextPage = () => {
    if (!data) return;
    setPage((p) => (p + 1 < data.totalPages ? p + 1 : p));
  };

  const handleSearchClick = () => {
    // ページを先頭に戻すだけ。日付等はすでに state が更新済み
    setPage(0);
  };

  return (
    <Container maxW="6xl" py={8}>
      <Stack spacing={6}>
        {/* ヘッダー */}
        <Box>
          <Heading size="lg">注文履歴</Heading>
          <Text mt={2} color="gray.500">
            期間を指定して約定済み注文の一覧とトータル損益を確認できます。
          </Text>
        </Box>

        {/* 検索条件フォーム */}
        <Box p={4} borderWidth="1px" borderRadius="lg" bg="gray.50">
          <VStack align="stretch" spacing={4}>
            <HStack spacing={4}>
              <FormControl>
                <FormLabel>開始日</FormLabel>
                <Input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </FormControl>

              <FormControl>
                <FormLabel>終了日</FormLabel>
                <Input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </FormControl>

              <FormControl maxW="150px">
                <FormLabel>ページサイズ</FormLabel>
                <NumberInput
                  min={1}
                  max={100}
                  value={size}
                  onChange={(_, valueAsNumber) => {
                    if (!Number.isNaN(valueAsNumber)) {
                      setSize(valueAsNumber);
                      setPage(0);
                    }
                  }}
                >
                  <NumberInputField />
                </NumberInput>
              </FormControl>
            </HStack>

            <HStack justify="flex-end">
              <Button
                onClick={handleSearchClick}
                colorScheme="blue"
                isDisabled={loading}
              >
                検索
              </Button>
            </HStack>
          </VStack>
        </Box>

        {/* サマリー（損益） */}
        <Box>
          <Stat
            p={4}
            borderWidth="1px"
            borderRadius="lg"
            shadow="sm"
            bg="white"
          >
            <StatLabel>期間損益</StatLabel>
            <StatNumber>
              {data ? data.profit.toLocaleString() : "-"} 円
            </StatNumber>
            <StatHelpText>
              対象期間内に決済済み注文の確定損益合計です。
            </StatHelpText>
          </Stat>
        </Box>

        <Divider />

        {/* ローディング / エラー */}
        {loading && (
          <HStack spacing={2}>
            <Spinner />
            <Text>読み込み中...</Text>
          </HStack>
        )}

        {error && (
          <Alert status="error">
            <AlertIcon />
            データの取得に失敗しました: {error.message}
          </Alert>
        )}

        {/* テーブル */}
        {!loading && data && (
          <Box borderWidth="1px" borderRadius="lg" overflowX="auto">
            <Table size="sm">
              <Thead bg="gray.100">
                <Tr>
                  <Th>ID</Th>
                  <Th>通貨ペア</Th>
                  <Th>売買</Th>
                  <Th isNumeric>サイズ</Th>
                  <Th>ステータス</Th>
                  <Th>約定日時</Th>
                  <Th isNumeric>約定価格</Th>
                  <Th>クローズ日時</Th>
                  <Th isNumeric>クローズ価格</Th>
                </Tr>
              </Thead>
              <Tbody>
                {data.orders.map((order) => (
                  <Tr key={order.orderId}>
                    <Td>{order.orderId}</Td>
                    <Td>{order.currencyPair}</Td>
                    <Td>
                      <Tag
                        colorScheme={order.side === "BUY" ? "green" : "red"}
                        size="sm"
                      >
                        {order.side}
                      </Tag>
                    </Td>
                    <Td isNumeric>{order.size.toLocaleString()}</Td>
                    <Td>
                      <Tag size="sm" variant="subtle" colorScheme="blue">
                        {order.status}
                      </Tag>
                    </Td>
                    <Td>
                      {order.fillDatetime
                        ? order.fillDatetime.replace("T", " ")
                        : "-"}
                    </Td>
                    <Td isNumeric>{order.fillPrice.toFixed(3)}</Td>
                    <Td>
                      {order.closeDatetime
                        ? order.closeDatetime.replace("T", " ")
                        : "-"}
                    </Td>
                    <Td isNumeric>
                      {order.closePrice !== null
                        ? order.closePrice.toFixed(3)
                        : "-"}
                    </Td>
                  </Tr>
                ))}
                {data.orders.length === 0 && (
                  <Tr>
                    <Td colSpan={9}>
                      <Text align="center" py={4} color="gray.500">
                        該当する注文はありません。
                      </Text>
                    </Td>
                  </Tr>
                )}
              </Tbody>
            </Table>
          </Box>
        )}

        {/* ページネーション */}
        {!loading && totalPages > 0 && (
          <HStack justify="space-between" mt={2}>
            <Text color="gray.600">
              全 {totalElements} 件 / {page + 1} ページ目（全 {totalPages}{" "}
              ページ）
            </Text>
            <HStack>
              <Button
                onClick={handlePrevPage}
                isDisabled={page === 0 || loading}
                variant="outline"
              >
                前へ
              </Button>
              <Button
                onClick={handleNextPage}
                isDisabled={page + 1 >= totalPages || loading}
                variant="outline"
              >
                次へ
              </Button>
            </HStack>
          </HStack>
        )}
      </Stack>
    </Container>
  );
};
