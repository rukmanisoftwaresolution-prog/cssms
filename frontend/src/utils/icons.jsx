import Dashboard from '@mui/icons-material/Dashboard';
import Category from '@mui/icons-material/Category';
import Badge from '@mui/icons-material/Badge';
import LocalShipping from '@mui/icons-material/LocalShipping';
import People from '@mui/icons-material/People';
import Landscape from '@mui/icons-material/Landscape';
import ShoppingCart from '@mui/icons-material/ShoppingCart';
import Inventory2 from '@mui/icons-material/Inventory2';
import PointOfSale from '@mui/icons-material/PointOfSale';
import Payments from '@mui/icons-material/Payments';
import AccountBalanceWallet from '@mui/icons-material/AccountBalanceWallet';
import ReceiptLong from '@mui/icons-material/ReceiptLong';
import AccountBalance from '@mui/icons-material/AccountBalance';
import Description from '@mui/icons-material/Description';
import CallReceived from '@mui/icons-material/CallReceived';
import CallMade from '@mui/icons-material/CallMade';
import Assessment from '@mui/icons-material/Assessment';
import Article from '@mui/icons-material/Article';
import TrendingUp from '@mui/icons-material/TrendingUp';
import Warehouse from '@mui/icons-material/Warehouse';
import SwapVert from '@mui/icons-material/SwapVert';
import ShowChart from '@mui/icons-material/ShowChart';
import ManageAccounts from '@mui/icons-material/ManageAccounts';

const icons = {
  Dashboard,
  Category,
  Badge,
  LocalShipping,
  People,
  Landscape,
  ShoppingCart,
  Inventory2,
  PointOfSale,
  Payments,
  AccountBalanceWallet,
  ReceiptLong,
  AccountBalance,
  Description,
  CallReceived,
  CallMade,
  Assessment,
  Article,
  TrendingUp,
  Warehouse,
  SwapVert,
  ShowChart,
  ManageAccounts,
};

export function getMenuIcon(name) {
  const Icon = icons[name] || Dashboard;
  return <Icon />;
}
