import { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Chip,
  Collapse,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import MenuIcon from '@mui/icons-material/Menu';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import PersonIcon from '@mui/icons-material/Person';
import LockResetIcon from '@mui/icons-material/LockReset';
import LogoutIcon from '@mui/icons-material/Logout';
import TerrainIcon from '@mui/icons-material/Terrain';
import { useAuth } from '../context/AuthContext';
import { filterMenu } from '../utils/constants';
import { getMenuIcon } from '../utils/icons';
import { humanize } from '../utils/formatters';

const DRAWER_WIDTH = 272;

function isActive(pathname, path) {
  if (path === '/') return pathname === '/';
  return pathname === path || pathname.startsWith(`${path}/`);
}

function SidebarContent({ menu, pathname, onNavigate }) {
  const [open, setOpen] = useState({});

  useEffect(() => {
    // auto-expand the group containing the active route
    menu.forEach((item) => {
      if (item.children?.some((c) => isActive(pathname, c.path))) {
        setOpen((o) => (o[item.label] ? o : { ...o, [item.label]: true }));
      }
    });
  }, [pathname, menu]);

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: 'primary.dark', color: '#fff' }}>
      <Toolbar sx={{ gap: 1.5 }}>
        <Avatar sx={{ bgcolor: 'secondary.main', width: 36, height: 36 }}>
          <TerrainIcon />
        </Avatar>
        <Box>
          <Typography variant="subtitle1" fontWeight={700} lineHeight={1.1}>
            CSSMS
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.7 }}>
            Crusher Stone Supply
          </Typography>
        </Box>
      </Toolbar>
      <Divider sx={{ borderColor: 'rgba(255,255,255,0.12)' }} />
      <List
        sx={{
          flex: 1,
          overflowY: 'auto',
          overflowX: 'hidden',
          scrollbarWidth: 'thin',
          scrollbarColor: 'rgba(255,255,255,0.38) #0f2f45',
          scrollbarGutter: 'stable',
          '&::-webkit-scrollbar': { width: 8 },
          '&::-webkit-scrollbar-track': { backgroundColor: '#0f2f45' },
          '&::-webkit-scrollbar-thumb': {
            backgroundColor: 'rgba(255,255,255,0.38)',
            border: '2px solid #0f2f45',
            borderRadius: 8,
          },
          '&::-webkit-scrollbar-thumb:hover': { backgroundColor: 'rgba(255,255,255,0.58)' },
          px: 1,
          py: 1,
        }}
        component="nav"
      >
        {menu.map((item) =>
          item.children ? (
            <Box key={item.label}>
              <ListItemButton
                onClick={() => setOpen((o) => ({ ...o, [item.label]: !o[item.label] }))}
                aria-expanded={!!open[item.label]}
                sx={itemSx(false)}
              >
                <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>{getMenuIcon(item.icon)}</ListItemIcon>
                <ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: 600 }} />
                {open[item.label] ? <ExpandLess /> : <ExpandMore />}
              </ListItemButton>
              <Collapse in={!!open[item.label]} timeout="auto" unmountOnExit>
                <List disablePadding>
                  {item.children.map((child) => (
                    <ListItemButton
                      key={child.path}
                      selected={isActive(pathname, child.path)}
                      onClick={() => onNavigate(child.path)}
                      sx={{ ...itemSx(isActive(pathname, child.path)), pl: 4 }}
                    >
                      <ListItemText primary={child.label} primaryTypographyProps={{ fontSize: 14 }} />
                    </ListItemButton>
                  ))}
                </List>
              </Collapse>
            </Box>
          ) : (
            <ListItemButton
              key={item.path}
              selected={isActive(pathname, item.path)}
              onClick={() => onNavigate(item.path)}
              sx={itemSx(isActive(pathname, item.path))}
            >
              <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>{getMenuIcon(item.icon)}</ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: 600 }} />
            </ListItemButton>
          )
        )}
      </List>
    </Box>
  );
}

const itemSx = (active) => ({
  borderRadius: 2,
  mb: 0.5,
  color: active ? '#fff' : 'rgba(255,255,255,0.78)',
  bgcolor: active ? 'rgba(255,255,255,0.16)' : 'transparent',
  '&:hover': { bgcolor: 'rgba(255,255,255,0.1)' },
  '&.Mui-selected, &.Mui-selected:hover': { bgcolor: 'rgba(255,255,255,0.16)' },
});

export default function MainLayout() {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();

  const menu = useMemo(() => filterMenu(role), [role]);

  const go = (path) => {
    setMobileOpen(false);
    setAnchorEl(null);
    navigate(path);
  };

  const handleLogout = async () => {
    setAnchorEl(null);
    await logout();
    navigate('/login', { replace: true });
  };

  const initials = (user?.fullName || user?.username || '?')
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  const drawer = <SidebarContent menu={menu} pathname={pathname} onNavigate={go} />;

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', width: '100%', minWidth: 0 }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          borderBottom: '1px solid',
          borderColor: 'divider',
          bgcolor: 'background.paper',
        }}
      >
        <Toolbar sx={{ gap: { xs: 0.5, sm: 1 }, px: { xs: 1.5, sm: 2 }, minHeight: { xs: 56, sm: 64 } }}>
          {!isDesktop && (
            <IconButton edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 1 }} aria-label="open menu">
              <MenuIcon />
            </IconButton>
          )}
          <Typography
            variant="h6"
            sx={{ flexGrow: 1, minWidth: 0, fontSize: { xs: '0.9rem', sm: '1.1rem', lg: '1.25rem' } }}
            noWrap
          >
            Crusher Stone Supply Management
          </Typography>
          <Chip
            size="small"
            label={humanize(role)}
            color="primary"
            variant="outlined"
            sx={{ mr: 1.5, display: { xs: 'none', sm: 'inline-flex' } }}
          />
          <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} aria-label="profile menu">
            <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36, fontSize: 14 }}>{initials}</Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <Box sx={{ px: 2, py: 1, minWidth: 200 }}>
              <Typography fontWeight={600}>{user?.fullName || user?.username}</Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.email}
              </Typography>
            </Box>
            <Divider />
            <MenuItem onClick={() => go('/profile')}>
              <ListItemIcon>
                <PersonIcon fontSize="small" />
              </ListItemIcon>
              Profile
            </MenuItem>
            <MenuItem onClick={() => go('/change-password')}>
              <ListItemIcon>
                <LockResetIcon fontSize="small" />
              </ListItemIcon>
              Change Password
            </MenuItem>
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: { xs: 'min(272px, 85vw)', sm: DRAWER_WIDTH }, border: 0 },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          open
          sx={{ display: { xs: 'none', md: 'block' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH, border: 0 } }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          minWidth: 0,
          maxWidth: '100%',
          p: { xs: 1.5, sm: 2, md: 3 },
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
