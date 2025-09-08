import { Flex, Heading, Link, Box } from "@chakra-ui/react";
import { FC, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export const Header: FC = () => {
    const {logout} = useAuth();
    const navigate = useNavigate();

    const onClickHome = useCallback(() => navigate("/home"), []);

    const onClickChart = useCallback(() => navigate("/home/chart"), []);

    const onClickSetting = useCallback(() => navigate("/home/setting"), []);

    const onClickLogOut = useCallback(() => {
        logout();
        navigate("/login");
    }, []);

    return (
        <>
            <Flex
                as="nav"
                bg="teal.500"
                color="gray.50"
                align="center"
                justify="space-between"
                padding={{ base: 3, md: 5 }}
            >
                <Flex 
                    align="center" 
                    as="a"
                    mr={8}
                    _hover={{ cusor: "pointer" }}
                    onClick={onClickHome}
                >
                    <Heading as="h1" fontSize={{ base: "md", md: "lg"}}>
                        Auto FX
                    </Heading>
                </Flex>
                <Flex
                    align="center"
                    fontSize="sm"
                    flexGrow={2}
                >
                    <Box pr={4}>
                        <Link onClick={onClickChart}>チャート</Link>
                    </Box>
                    <Box pr={4}>
                        <Link onClick={onClickSetting}>トレード設定</Link>
                    </Box>
                    <Box pr={4}>
                        <Link onClick={onClickLogOut}>ログアウト</Link>
                    </Box>
                </Flex>
            </Flex>
        </>
    );
};