import { Charts } from "../components/pages/Charts";
import { TradeSetting } from "../components/pages/TradeSetting";
import { OrderHistoryPage } from "../components/pages/OrderHistoryPage";

export const homeRoutes = [
    {
        path: "chart",
        index: true,
        children: <Charts />,
    },
    {
        path: "setting",
        index: false,
        children: <TradeSetting />,
    },
    {
        path: "orders",
        index: false,
        children: <OrderHistoryPage />
    },
];