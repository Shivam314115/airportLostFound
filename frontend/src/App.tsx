import { useState, useEffect, useCallback, type FormEvent, type ReactNode } from 'react';
import {
  ArrowRight,
  ArrowUpRight,
  BadgeHelp,
  CheckCircle2,
  ChevronDown,
  CircleAlert,
  FileText,
  Layers,
  LogOut,
  Luggage,
  MapPin,
  Menu,
  Plane,
  Plus,
  Search,
  ShieldCheck,
  Smartphone,
  User,
  X,
  type LucideIcon,
} from 'lucide-react';
import Dialog from './components/ui/Dialog';
import AirplaneCanvas from './components/AirplaneCanvas';
import StaffWorkspace from './components/StaffWorkspace';
import { ImageUploadWidget } from './components/ImageUploadWidget';
import {
  getCurrentUser,
  getLostReports,
  getMatches,
  getReferenceData,
  login,
  logout,
  register,
  createLostReport,
  type CategoryOption,
  type LocationOption,
  type FlightOption,
  type LostReport,
  type MatchRecord,
  type PassengerProfile,
  type PhotoAttachment,
} from './api/client';

type View = 'discover' | 'my-reports' | 'matches' | 'staff';

function BrandLogo() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg
        className="brand-logo-svg"
        viewBox="0 0 44 44"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id="brand-bg" x1="0" y1="0" x2="44" y2="44" gradientUnits="userSpaceOnUse">
            <stop stopColor="#0B233A" />
            <stop offset="0.6" stopColor="#06182A" />
            <stop offset="1" stopColor="#030E1A" />
          </linearGradient>
          <linearGradient id="brand-arc" x1="6" y1="37" x2="32" y2="12" gradientUnits="userSpaceOnUse">
            <stop stopColor="#0284C7" stopOpacity="0" />
            <stop offset="0.45" stopColor="#0284C7" stopOpacity="0.5" />
            <stop offset="1" stopColor="#38BDF8" />
          </linearGradient>
          <linearGradient id="brand-plane-body" x1="12" y1="30" x2="32" y2="12" gradientUnits="userSpaceOnUse">
            <stop stopColor="#FFFFFF" />
            <stop offset="1" stopColor="#F0F9FF" />
          </linearGradient>
        </defs>

        {/* Base Squircle with Subtle Rim Highlight */}
        <rect width="44" height="44" rx="12" fill="url(#brand-bg)" />
        <rect x="0.75" y="0.75" width="42.5" height="42.5" rx="11.25" stroke="rgba(255, 255, 255, 0.16)" strokeWidth="1" />

        {/* Radar Guidance Arc / Contrail */}
        <path
          d="M 6 37 C 11 30, 18 23, 29 14"
          stroke="url(#brand-arc)"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeDasharray="2.5 2"
        />

        {/* Arrival Waypoint Beacon */}
        <circle cx="33" cy="11" r="5" stroke="#38BDF8" strokeOpacity="0.3" strokeWidth="1" strokeDasharray="2 2" />
        <circle cx="33" cy="11" r="2.2" fill="#38BDF8" />
        <circle cx="33" cy="11" r="0.9" fill="#FFFFFF" />

        {/* Commercial Jetliner (Ascending at 42° towards beacon) */}
        <path
          d="M 28.2 14.1 C 28.7 13.6 28.5 12.9 27.9 12.6 C 27.2 12.4 26.4 12.6 25.8 13.2 L 22.0 17.0 L 12.6 14.8 C 11.9 14.6 11.3 15.1 11.5 15.8 L 12.3 16.9 C 12.6 17.4 13.1 17.7 13.7 17.8 L 19.1 19.1 L 16.6 22.2 L 13.4 21.9 C 12.9 21.8 12.5 22.2 12.4 22.7 L 12.2 23.3 C 12.1 23.7 12.3 24.1 12.7 24.2 L 15.5 25.0 L 16.3 27.8 C 16.5 28.3 16.9 28.5 17.3 28.4 L 17.9 28.2 C 18.4 28.0 18.7 27.6 18.6 27.1 L 18.3 23.9 L 21.4 21.4 L 22.7 26.8 C 22.8 27.4 23.2 27.9 23.7 28.2 L 24.8 28.9 C 25.5 29.1 26.0 28.5 25.8 27.8 L 23.7 18.4 L 27.3 14.9 C 27.7 14.5 28.0 14.3 28.2 14.1 Z"
          fill="url(#brand-plane-body)"
        />
      </svg>
    </span>
  );
}

function Brand({ onClick }: { onClick: () => void }) {
  return (
    <button className="brand" onClick={onClick} aria-label="Airport Lost & Found, return to discover">
      <BrandLogo />
      <span className="brand-copy">
        <span className="brand-title">
          <span className="brand-title-primary">Airport</span>
          <span className="brand-title-secondary">Lost &amp; Found</span>
        </span>
        <span className="brand-subtitle">
          <span className="brand-pulse-dot" />
          AIRPORT LOST PROPERTY
        </span>
      </span>
    </button>
  );
}

function Field({ label, required, hint, children }: {
  label: string;
  required?: boolean;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="field">
      <span className="field-label">{label}{required && <span className="required-mark"> *</span>}</span>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}

function EmptyState({
  icon: Icon,
  eyebrow,
  title,
  description,
  action,
}: {
  icon: LucideIcon;
  eyebrow: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <section className="empty-state" aria-live="polite">
      <div className="empty-illustration" aria-hidden="true">
        <span className="empty-orbit empty-orbit-one" />
        <span className="empty-orbit empty-orbit-two" />
        <span className="empty-icon-wrap"><Icon size={27} strokeWidth={1.6} /></span>
        <span className="empty-signal" />
      </div>
      <p className="eyebrow">{eyebrow}</p>
      <h3>{title}</h3>
      <p className="empty-description">{description}</p>
      {action && <div className="empty-actions">{action}</div>}
    </section>
  );
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status.toLowerCase();
  let badgeClass = 'status-candidate';
  let label = status;

  if (normalized === 'open') {
    badgeClass = 'status-open';
    label = 'Open Report';
  } else if (normalized === 'candidate_found') {
    badgeClass = 'status-candidate';
    label = 'Candidate Found';
  } else if (normalized === 'resolved' || normalized === 'verified') {
    badgeClass = 'status-resolved';
    label = normalized === 'verified' ? 'Verified Match' : 'Resolved';
  } else if (normalized === 'candidate') {
    badgeClass = 'status-candidate';
    label = 'Candidate Match';
  }

  return (
    <span className={`status-badge ${badgeClass}`}>
      <span className="status-badge-dot" />
      {label}
    </span>
  );
}

export default function App() {
  const [activeView, setActiveView] = useState<View>('discover');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('All categories');
  const [statusFilter, setStatusFilter] = useState<string>('All');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  // Modals
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [loginRoleTab, setLoginRoleTab] = useState<'passenger' | 'staff'>('passenger');
  const [reportOpen, setReportOpen] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);

  // Feedback notifications
  const [authError, setAuthError] = useState('');
  const [authSuccess, setAuthSuccess] = useState('');
  const [reportError, setReportError] = useState('');
  const [reportSuccess, setReportSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Live Database State
  const [currentUser, setCurrentUser] = useState<PassengerProfile | null>(null);
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [locations, setLocations] = useState<LocationOption[]>([]);
  const [flights, setFlights] = useState<FlightOption[]>([]);
  const [reports, setReports] = useState<LostReport[]>([]);
  const [matches, setMatches] = useState<MatchRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Forms
  const [loginForm, setLoginForm] = useState({ email: '', password: '' });
  const [registerForm, setRegisterForm] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [reportForm, setReportForm] = useState({
    itemName: '',
    categoryId: 1,
    brand: '',
    colour: '',
    description: '',
    itemCondition: 'good' as const,
    estimatedValue: '',
    lostAt: new Date().toISOString().slice(0, 16),
    locationId: 1,
    flightId: '' as number | string,
    additionalDetails: '',
    photos: [] as PhotoAttachment[],
  });
  const [lightboxImg, setLightboxImg] = useState<string | null>(null);

  // Load Reference Data (Categories, Locations, Flights) & Initial Session & Reports
  const initData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [refData, userProfile, allReports] = await Promise.all([
        getReferenceData().catch(() => ({ categories: [], locations: [], flights: [] })),
        getCurrentUser().catch(() => null),
        getLostReports().catch(() => []),
      ]);

      setCategories(refData.categories);
      setLocations(refData.locations);
      setFlights(refData.flights);
      setCurrentUser(userProfile);
      setReports(allReports);

      // If user is authenticated, also load matches
      if (userProfile) {
        const allMatches = await getMatches().catch(() => []);
        setMatches(allMatches);
      }
    } catch (err) {
      console.error('Initialization error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    initData();
  }, [initData]);

  // Reload reports when user changes or report is filed
  const refreshReports = async () => {
    try {
      const res = await getLostReports();
      setReports(res);
      if (currentUser) {
        const matchRes = await getMatches().catch(() => []);
        setMatches(matchRes);
      }
    } catch (err) {
      console.error('Refresh reports error:', err);
    }
  };

  const navigate = (view: View) => {
    setActiveView(view);
    setMobileNavOpen(false);
    setSearchQuery('');
    setSelectedCategory('All categories');
    setStatusFilter('All');
  };

  const handleOpenReport = () => {
    if (!currentUser) {
      setAuthMode('login');
      setAuthError('Please sign in to lodge an official lost property report.');
      setAuthOpen(true);
      return;
    }
    setReportError('');
    setReportSuccess('');
    setReportOpen(true);
  };

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthError('');
    setIsSubmitting(true);
    try {
      const email = loginForm.email.trim();
      const profile = await login({ email, password: loginForm.password });
      setCurrentUser(profile);
      setAuthOpen(false);
      setLoginForm({ email: '', password: '' });
      if (profile.userType === 'staff') {
        setActiveView('staff');
      }
      refreshReports();
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegisterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthError('');
    setIsSubmitting(true);
    try {
      const cleanData = {
        fullName: registerForm.fullName.trim(),
        email: registerForm.email.trim(),
        phone: registerForm.phone.trim() || undefined,
        password: registerForm.password,
      };
      await register(cleanData);
      // Automatically log in after registration
      const profile = await login({ email: cleanData.email, password: cleanData.password });
      setCurrentUser(profile);
      setAuthOpen(false);
      setRegisterForm({ fullName: '', email: '', phone: '', password: '' });
      refreshReports();
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      setCurrentUser(null);
      setMatches([]);
      if (activeView === 'my-reports') {
        navigate('discover');
      }
    } catch (err) {
      console.error('Logout error:', err);
    }
  };

  const handleReportSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setReportError('');
    setReportSuccess('');
    setIsSubmitting(true);

    try {
      // Format timestamp to ISO with timezone offset
      const isoLostAt = new Date(reportForm.lostAt).toISOString();

      await createLostReport({
        categoryId: Number(reportForm.categoryId),
        itemName: reportForm.itemName.trim(),
        brand: reportForm.brand.trim() || undefined,
        colour: reportForm.colour.trim() || undefined,
        description: reportForm.description.trim() || undefined,
        estimatedValue: reportForm.estimatedValue ? Number(reportForm.estimatedValue) : undefined,
        itemCondition: reportForm.itemCondition,
        lostAt: isoLostAt,
        flightId: reportForm.flightId ? Number(reportForm.flightId) : undefined,
        lastSeenLocationId: reportForm.locationId ? Number(reportForm.locationId) : undefined,
        additionalDetails: reportForm.additionalDetails.trim() || undefined,
        photos: reportForm.photos && reportForm.photos.length > 0 ? reportForm.photos : undefined,
      });

      setReportSuccess('Report successfully lodged in the airport register.');
      await refreshReports();
      setTimeout(() => {
        setReportOpen(false);
        setReportSuccess('');
        setReportForm({
          itemName: '',
          categoryId: categories[0]?.categoryId || 1,
          brand: '',
          colour: '',
          description: '',
          itemCondition: 'good',
          estimatedValue: '',
          lostAt: new Date().toISOString().slice(0, 16),
          locationId: locations[0]?.locationId || 1,
          flightId: '' as number | string,
          additionalDetails: '',
          photos: [],
        });
        navigate('my-reports');
      }, 1200);
    } catch (err) {
      setReportError(err instanceof Error ? err.message : 'Failed to lodge report.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Filtered reports for discovery view
  const filteredReports = reports.filter((rep) => {
    const matchesSearch =
      !searchQuery ||
      rep.itemName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      rep.categoryName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (rep.lastSeenLocation && rep.lastSeenLocation.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (rep.flightNumber && rep.flightNumber.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (rep.additionalDetails && rep.additionalDetails.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesCat =
      selectedCategory === 'All categories' ||
      rep.categoryName.toLowerCase() === selectedCategory.toLowerCase();

    const matchesStatus =
      statusFilter === 'All' ||
      rep.reportStatus.toLowerCase() === statusFilter.toLowerCase();

    return matchesSearch && matchesCat && matchesStatus;
  });

  // Filtered reports for passenger view
  const myReports = currentUser
    ? reports.filter((rep) => rep.passengerId === currentUser.passengerId)
    : [];

  // Helper icons for categories
  const getCategoryIcon = (name: string): LucideIcon => {
    const n = name.toLowerCase();
    if (n.includes('elect')) return Smartphone;
    if (n.includes('doc')) return FileText;
    if (n.includes('bag')) return Luggage;
    return Layers;
  };

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Skip to content</a>
      <header className="site-header">
        <div className="header-inner">
          <Brand onClick={() => navigate('discover')} />

          <nav className={`main-nav${mobileNavOpen ? ' nav-open' : ''}`} aria-label="Main navigation">
            <button
              className={`nav-link${activeView === 'discover' ? ' nav-link-active' : ''}`}
              onClick={() => navigate('discover')}
            >
              Search Items
            </button>
            {currentUser?.userType === 'staff' ? (
              <button
                className={`nav-link${activeView === 'staff' ? ' nav-link-active' : ''}`}
                onClick={() => navigate('staff')}
              >
                Staff Dashboard
                <span className="staff-role-badge">{currentUser.staffRoleTitle}</span>
              </button>
            ) : (
              <>
                <button
                  className={`nav-link${activeView === 'my-reports' ? ' nav-link-active' : ''}`}
                  onClick={() => navigate('my-reports')}
                >
                  My Reports {currentUser && myReports.length > 0 && `(${myReports.length})`}
                </button>
                <button
                  className={`nav-link${activeView === 'matches' ? ' nav-link-active' : ''}`}
                  onClick={() => navigate('matches')}
                >
                  Matches {currentUser && matches.length > 0 && `(${matches.length})`}
                </button>
              </>
            )}
          </nav>

          <div className="header-actions">
            <button className="header-help" onClick={() => setHelpOpen(true)}>
              <BadgeHelp size={17} />
              <span>Help</span>
            </button>

            {currentUser ? (
              <div className="user-chip">
                <span className="user-avatar" aria-hidden="true">
                  {currentUser.fullName.charAt(0).toUpperCase()}
                </span>
                <span>{currentUser.fullName}</span>
                {currentUser.staffRoleTitle && (
                  <span className="staff-role-badge">{currentUser.staffRoleTitle}</span>
                )}
                <button
                  className="sign-out-btn"
                  onClick={handleLogout}
                  title="Sign out of session"
                  aria-label="Sign out"
                >
                  <LogOut size={14} />
                </button>
              </div>
            ) : (
              <button
                className="sign-in-button"
                onClick={() => {
                  setAuthMode('login');
                  setAuthError('');
                  setAuthOpen(true);
                }}
              >
                Sign in <ArrowUpRight size={15} />
              </button>
            )}

            {currentUser?.userType !== 'staff' && (
              <button className="button button-primary header-report" onClick={handleOpenReport}>
                <Plus size={17} />
                <span>Report Lost Item</span>
              </button>
            )}
          </div>

          <button
            className="mobile-menu-button icon-button"
            aria-label={mobileNavOpen ? 'Close navigation' : 'Open navigation'}
            aria-expanded={mobileNavOpen}
            onClick={() => setMobileNavOpen((open) => !open)}
          >
            {mobileNavOpen ? <X size={21} /> : <Menu size={21} />}
          </button>
        </div>
      </header>

      <main id="main-content">
        {/* ===================================================================
            VIEW 1: SEARCH & BROWSE ITEMS
           =================================================================== */}
        {activeView === 'discover' && (
          <>
            <section className="hero-section page-wrap">
              <div className="hero-copy">
                <p className="eyebrow">
                  <span className="eyebrow-rule" /> AIRPORT LOST &amp; FOUND
                </p>
                <h1>
                  Lost something<br />at the airport<span className="hero-period">?</span>
                </h1>
                <p className="hero-intro">
                  Search through found items or submit a report to quickly get your lost belongings back.
                </p>
                <div className="hero-actions">
                  <button
                    className="button button-primary button-large"
                    onClick={() => document.getElementById('item-search')?.focus()}
                  >
                    <Search size={16} />
                    <span>Search found items</span>
                    <ArrowRight size={17} />
                  </button>
                  <button className="text-link" onClick={handleOpenReport}>
                    <span>Report a lost item</span>
                    <ArrowUpRight size={16} />
                  </button>
                </div>
              </div>

              <aside className="hero-3d-stage" aria-label="Airport 3D Airplane">
                <svg className="hero-orbit-svg" viewBox="0 0 740 460" fill="none" aria-hidden="true">
                  <ellipse
                    cx="375"
                    cy="235"
                    rx="325"
                    ry="125"
                    stroke="#bce5ec"
                    strokeWidth="1.6"
                    strokeDasharray="6 8"
                    transform="rotate(-9 375 235)"
                  />
                  {/* Waypoint 1 */}
                  <g transform="translate(62, 230)">
                    <circle cx="0" cy="0" r="14" fill="#087f89" />
                    <circle cx="0" cy="0" r="5" fill="#ffffff" />
                  </g>
                  {/* Waypoint 2 */}
                  <g transform="translate(255, 110)">
                    <circle cx="0" cy="0" r="14" fill="#087f89" />
                    <circle cx="0" cy="0" r="5" fill="#ffffff" />
                  </g>
                  {/* Waypoint 3 */}
                  <g transform="translate(685, 175)">
                    <circle cx="0" cy="0" r="14" fill="#087f89" />
                    <circle cx="0" cy="0" r="5" fill="#ffffff" />
                  </g>
                </svg>

                <AirplaneCanvas />
              </aside>
            </section>

            <div className="hero-divider page-wrap" />

            <section className="discovery-section page-wrap" aria-labelledby="discovery-title">
              <div className="section-intro">
                <div>
                  <p className="eyebrow"><span className="eyebrow-rule" /> ITEMS DATABASE</p>
                  <h2 id="discovery-title">Search items<span className="heading-period">.</span></h2>
                  <p className="section-description">Filter by item name, flight, or airport location.</p>
                </div>
                <span className="data-state">
                  <span className="data-state-dot" style={{ background: '#1b8a5b' }} />
                  {reports.length} items logged
                </span>
              </div>

              <div className="search-panel">
                <form className="search-form" onSubmit={(e) => e.preventDefault()} role="search">
                  <Search className="search-icon" size={21} strokeWidth={1.8} />
                  <input
                    id="item-search"
                    type="search"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by item name, brand, flight, or location..."
                    aria-label="Search items"
                  />
                  {searchQuery && (
                    <button type="button" className="clear-search" onClick={() => setSearchQuery('')} aria-label="Clear search">
                      <X size={16} />
                    </button>
                  )}
                  <span className="search-divider" />
                  <button type="button" className="button button-primary search-button">
                    Search <ArrowRight size={16} />
                  </button>
                </form>

                <div className="search-divider-subtle" />

                <div className="search-controls">
                  <div className="category-scroller" role="group" aria-label="Filter by item category">
                    <button
                      className={`category-chip${selectedCategory === 'All categories' ? ' category-chip-active' : ''}`}
                      onClick={() => setSelectedCategory('All categories')}
                    >
                      <Layers size={14} />
                      All categories
                    </button>
                    {categories.map((cat) => {
                      const Icon = getCategoryIcon(cat.categoryName);
                      return (
                        <button
                          key={cat.categoryId}
                          className={`category-chip${selectedCategory === cat.categoryName ? ' category-chip-active' : ''}`}
                          onClick={() => setSelectedCategory(cat.categoryName)}
                        >
                          <Icon size={14} strokeWidth={1.8} />
                          {cat.categoryName}
                        </button>
                      );
                    })}
                  </div>

                  <span className="filter-context">
                    Showing: {selectedCategory} <ChevronDown size={14} />
                  </span>
                </div>
              </div>

              <div className="results-heading">
                <div>
                  <h3>Recent Items</h3>
                  <p>Items reported by passengers and staff</p>
                </div>
                <span className="records-count">
                  <strong>{filteredReports.length}</strong> {filteredReports.length === 1 ? 'record' : 'records'}
                </span>
              </div>

              {/* Live Reports Grid */}
              {isLoading ? (
                <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--muted)' }}>
                  Loading registered items...
                </div>
              ) : filteredReports.length > 0 ? (
                <div className="reports-grid">
                  {filteredReports.map((item) => (
                    <article key={item.lostReportId} className="report-card">
                      <div>
                        <div className="card-top">
                          <div className="card-meta-left">
                            <span className="category-tag">
                              {item.categoryName}
                            </span>
                            <h4 className="card-title">{item.itemName}</h4>
                          </div>
                          <StatusBadge status={item.reportStatus} />
                        </div>

                        <div className="card-details">
                          {item.lastSeenLocation && (
                            <div className="detail-row">
                              <MapPin size={15} />
                              <span>Last seen: <strong>{item.lastSeenLocation}</strong></span>
                            </div>
                          )}
                          {item.flightNumber && (
                            <div className="detail-row">
                              <Plane size={15} />
                              <span>Flight: <strong>{item.flightNumber}</strong></span>
                            </div>
                          )}
                          <div className="detail-row">
                            <User size={15} />
                            <span>Passenger: <strong>{item.passengerName}</strong></span>
                          </div>
                          {item.additionalDetails && (
                            <p className="card-description">{item.additionalDetails}</p>
                          )}

                          {item.photoUrls && item.photoUrls.length > 0 && (
                            <div className="report-card-media">
                              {item.photoUrls.map((url, pIdx) => (
                                <div
                                  key={pIdx}
                                  className="report-card-thumb-wrap"
                                  onClick={() => setLightboxImg(url)}
                                  title="Click to view photograph"
                                >
                                  <img
                                    src={url}
                                    alt={`${item.itemName} photograph ${pIdx + 1}`}
                                    className="report-card-thumb"
                                    loading="lazy"
                                  />
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="card-footer">
                        <span>Report #{item.lostReportId}</span>
                        <span>
                          Lost: {new Date(item.lostAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                        </span>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={Search}
                  eyebrow="NO MATCHING RECORDS"
                  title="No items found matching your filters."
                  description={`No registered items matched "${searchQuery || selectedCategory}". Try selecting another category or clear your search.`}
                  action={
                    <button
                      className="button button-secondary"
                      onClick={() => {
                        setSearchQuery('');
                        setSelectedCategory('All categories');
                      }}
                    >
                      Clear search filters
                    </button>
                  }
                />
              )}
            </section>
          </>
        )}

        {/* ===================================================================
            VIEW 2: PASSENGER AREA (MY REPORTS)
           =================================================================== */}
        {activeView === 'my-reports' && (
          <section className="workspace page-wrap">
            <div className="workspace-heading">
              <div>
                <p className="eyebrow"><span className="eyebrow-rule" /> MY REPORTS</p>
                <h1>My reports<span className="heading-period">.</span></h1>
                <p className="section-description">
                  {currentUser
                    ? `Track the status of items reported under ${currentUser.fullName}.`
                    : 'Track the status of your reported items.'}
                </p>
              </div>
              <button className="button button-primary" onClick={handleOpenReport}>
                <Plus size={16} /> Report Lost Item
              </button>
            </div>

            {!currentUser ? (
              <EmptyState
                icon={FileText}
                eyebrow="SIGN IN REQUIRED"
                title="Sign in to view your reports."
                description="Sign in or create an account to view your reports and track matching items."
                action={
                  <button
                    className="button button-primary"
                    onClick={() => {
                      setAuthMode('login');
                      setAuthError('');
                      setAuthOpen(true);
                    }}
                  >
                    Sign in to your account <ArrowRight size={16} />
                  </button>
                }
              />
            ) : myReports.length > 0 ? (
              <div className="reports-grid">
                {myReports.map((item) => (
                  <article key={item.lostReportId} className="report-card">
                    <div>
                      <div className="card-top">
                        <div className="card-meta-left">
                          <span className="category-tag">{item.categoryName}</span>
                          <h4 className="card-title">{item.itemName}</h4>
                        </div>
                        <StatusBadge status={item.reportStatus} />
                      </div>

                      <div className="card-details">
                        {item.lastSeenLocation && (
                          <div className="detail-row">
                            <MapPin size={15} />
                            <span>Location: <strong>{item.lastSeenLocation}</strong></span>
                          </div>
                        )}
                        {item.flightNumber && (
                          <div className="detail-row">
                            <Plane size={15} />
                            <span>Flight: <strong>{item.flightNumber}</strong></span>
                          </div>
                        )}
                        {item.additionalDetails && (
                          <p className="card-description">{item.additionalDetails}</p>
                        )}

                        {item.photoUrls && item.photoUrls.length > 0 && (
                          <div className="report-card-media">
                            {item.photoUrls.map((url, pIdx) => (
                              <div
                                key={pIdx}
                                className="report-card-thumb-wrap"
                                onClick={() => setLightboxImg(url)}
                                title="Click to view photograph"
                              >
                                <img
                                  src={url}
                                  alt={`${item.itemName} photograph ${pIdx + 1}`}
                                  className="report-card-thumb"
                                  loading="lazy"
                                />
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="card-footer">
                      <span>Report #{item.lostReportId}</span>
                      <span>
                        Reported: {new Date(item.reportedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                      </span>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState
                icon={FileText}
                eyebrow="NO REPORTS YET"
                title="You have no active reports."
                description="You haven't reported any lost items yet. If you left something behind, submit a report to get started."
                action={
                  <button className="button button-primary" onClick={handleOpenReport}>
                    <Plus size={16} /> Report a lost item
                  </button>
                }
              />
            )}

            <div className="protocol-grid">
              <div className="protocol-card">
                <span className="protocol-step-num">STEP 01</span>
                <h4>Report Lost Item</h4>
                <p>Submit details and photos of what you lost, including flight or location.</p>
              </div>
              <div className="protocol-card">
                <span className="protocol-step-num">STEP 02</span>
                <h4>Automatic Match</h4>
                <p>The system automatically checks for matching items reported by airport staff.</p>
              </div>
              <div className="protocol-card">
                <span className="protocol-step-num">STEP 03</span>
                <h4>Safe Storage</h4>
                <p>Found items are safely secured in airport storage lockers.</p>
              </div>
              <div className="protocol-card">
                <span className="protocol-step-num">STEP 04</span>
                <h4>Verify &amp; Return</h4>
                <p>Verify your identity at the airport counter to collect your item.</p>
              </div>
            </div>
          </section>
        )}

        {/* ===================================================================
            VIEW 3: POSSIBLE MATCHES
           =================================================================== */}
        {activeView === 'matches' && (
          <section className="workspace page-wrap">
            <div className="workspace-heading">
              <div>
                <p className="eyebrow"><span className="eyebrow-rule" /> ITEM MATCHES</p>
                <h1>Matches<span className="heading-period">.</span></h1>
                <p className="section-description">
                  Items found by airport staff that match your lost reports.
                </p>
              </div>
            </div>

            {!currentUser ? (
              <EmptyState
                icon={CircleAlert}
                eyebrow="SIGN IN REQUIRED"
                title="Sign in to review matches."
                description="Sign in to view items matching your lost reports."
                action={
                  <button
                    className="button button-primary"
                    onClick={() => {
                      setAuthMode('login');
                      setAuthError('');
                      setAuthOpen(true);
                    }}
                  >
                    Sign in to view matches <ArrowRight size={16} />
                  </button>
                }
              />
            ) : matches.length > 0 ? (
              <div className="matches-grid">
                {matches.map((m, idx) => (
                  <article key={idx} className="match-card">
                    <div>
                      <div className="card-top">
                        <span className="match-score-pill">
                          {Number(m.matchScore).toFixed(1)}% Match Score
                        </span>
                        <StatusBadge status={m.matchStatus} />
                      </div>

                      <div className="match-comparison">
                        <div className="match-side">
                          <span className="match-side-label">Lost Item</span>
                          <strong>{m.lostItemName}</strong>
                          <small style={{ color: 'var(--muted)' }}>Report #{m.lostReportId}</small>
                        </div>
                        <ArrowRight size={18} className="match-arrow" />
                        <div className="match-side" style={{ textAlign: 'right' }}>
                          <span className="match-side-label">Found Custody</span>
                          <strong>{m.foundItemName}</strong>
                          <small style={{ color: 'var(--muted)' }}>Item #{m.foundItemId}</small>
                        </div>
                      </div>

                      <div className="card-details">
                        <div className="detail-row">
                          <MapPin size={15} />
                          <span>Recovered at: <strong>{m.foundLocation}</strong></span>
                        </div>
                        <div className="detail-row">
                          <User size={15} />
                          <span>Passenger: <strong>{m.passengerName}</strong></span>
                        </div>
                        {m.reviewedBy && (
                          <div className="detail-row">
                            <ShieldCheck size={15} />
                            <span>Reviewed by: <strong>{m.reviewedBy}</strong></span>
                          </div>
                        )}
                        {m.matchRemarks && (
                          <div className="match-remarks">
                            <strong>Officer Note:</strong> {m.matchRemarks}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="card-footer">
                      <span>Status: {m.matchStatus}</span>
                      <span>
                        Found: {new Date(m.foundAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                      </span>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <EmptyState
                icon={CircleAlert}
                eyebrow="NO CANDIDATE MATCHES"
                title="No current match records."
                description="There are currently no candidate matches pending review for your account. When terminal custody staff log an item that correlates with your report, it will appear here."
                action={
                  <button className="button button-secondary" onClick={() => navigate('discover')}>
                    Return to discovery <ArrowRight size={16} />
                  </button>
                }
              />
            )}
          </section>
        )}

        {/* ===================================================================
            VIEW 4: AIRPORT STAFF WORKSPACE (Role-Based Subrole Control)
           =================================================================== */}
        {activeView === 'staff' && (
          currentUser?.userType === 'staff' ? (
            <StaffWorkspace
              currentUser={currentUser}
              referenceData={{ categories, locations, flights }}
              onRefreshPassengerData={refreshReports}
            />
          ) : (
            <div className="reports-page-wrapper">
              <EmptyState
                icon={CircleAlert}
                eyebrow="RESTRICTED TERMINAL"
                title="Airport Staff Authentication Required"
                description="The Staff Workspace is reserved for authorized airport personnel across intake, verification, inventory, and administrative roles."
                action={
                  <button
                    className="button button-primary"
                    onClick={() => {
                      setAuthMode('login');
                      setLoginRoleTab('staff');
                      setAuthError('');
                      setAuthOpen(true);
                    }}
                  >
                    Sign in with Staff Credentials <ArrowRight size={16} />
                  </button>
                }
              />
            </div>
          )
        )}
      </main>

      <footer className="site-footer">
        <div className="footer-container">
          <div className="footer-main">
            <div className="footer-brand-col">
              <Brand onClick={() => navigate('discover')} />
              <p className="footer-desc">
                Airport service connecting passenger lost reports with found property custody.
              </p>
            </div>

            <div className="footer-nav-col">
              <span className="footer-col-title">Passenger</span>
              <ul className="footer-links">
                <li><button type="button" onClick={() => navigate('discover')}>Search Items</button></li>
                <li><button type="button" onClick={() => navigate('my-reports')}>My Reports</button></li>
                <li><button type="button" onClick={() => navigate('matches')}>Matches</button></li>
                <li><button type="button" onClick={handleOpenReport}>+ Report Lost Item</button></li>
              </ul>
            </div>

            <div className="footer-nav-col">
              <span className="footer-col-title">Airport Desks</span>
              <ul className="footer-links">
                <li><span>Indore (IDR) &middot; Terminal 1</span></li>
                <li><span>Mumbai (BOM) &middot; Terminal 2</span></li>
                <li>
                  <button type="button" className="footer-help-btn" onClick={() => setHelpOpen(true)}>
                    <BadgeHelp size={14} /> How it Works
                  </button>
                </li>
              </ul>
            </div>

            <div className="footer-nav-col">
              <span className="footer-col-title">Staff Portal</span>
              <ul className="footer-links">
                <li><button type="button" onClick={() => navigate('staff')}>Staff Dashboard</button></li>
                <li><span>Intake &amp; Verification</span></li>
                <li><span>Storage Lockers</span></li>
              </ul>
            </div>
          </div>

          <div className="footer-divider" />

          <div className="footer-bottom-bar">
            <div className="footer-copyright">
              &copy; {new Date().getFullYear()} <strong>Airport Lost &amp; Found</strong>
            </div>
          </div>
        </div>
      </footer>

      {/* ===================================================================
          MODAL 1: AUTHENTICATION (SIGN IN & REGISTER)
         =================================================================== */}
      <Dialog
        open={authOpen}
        onOpenChange={setAuthOpen}
        title={authMode === 'login' ? 'Sign in to AirportLostandFound' : 'Create Passenger Account'}
        description={
          authMode === 'login'
            ? 'Sign in to access your reports, track custody, and lodge property claims.'
            : 'Register your details to lodge reports and receive automated match alerts.'
        }
        className="auth-dialog"
      >
        <div className="auth-tabs" role="tablist">
          <button
            type="button"
            className={`auth-tab${authMode === 'login' ? ' auth-tab-active' : ''}`}
            onClick={() => { setAuthMode('login'); setAuthError(''); setAuthSuccess(''); }}
          >
            Sign in
          </button>
          <button
            type="button"
            className={`auth-tab${authMode === 'register' ? ' auth-tab-active' : ''}`}
            onClick={() => { setAuthMode('register'); setAuthError(''); setAuthSuccess(''); }}
          >
            Create account
          </button>
        </div>

        {authError && (
          <div className="status-toast status-toast-error" role="alert">
            <CircleAlert size={18} />
            <span>{authError}</span>
          </div>
        )}
        {authSuccess && (
          <div className="status-toast status-toast-success" role="status">
            <CheckCircle2 size={18} />
            <span>{authSuccess}</span>
          </div>
        )}

        {authMode === 'login' ? (
          <div>
            {/* Sub-toggle: Passenger vs Staff */}
            <div className="auth-subtabs" style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
              <button
                type="button"
                className={`button button-small ${loginRoleTab === 'passenger' ? 'button-primary' : 'button-outline'}`}
                style={{ flex: 1 }}
                onClick={() => {
                  setLoginRoleTab('passenger');
                  setLoginForm({ email: '', password: '' });
                }}
              >
                Passenger
              </button>
              <button
                type="button"
                className={`button button-small ${loginRoleTab === 'staff' ? 'button-primary' : 'button-outline'}`}
                style={{ flex: 1 }}
                onClick={() => {
                  setLoginRoleTab('staff');
                  setLoginForm({ email: '', password: '' });
                }}
              >
                Airport Staff
              </button>
            </div>

            <form className="dialog-form" onSubmit={handleLoginSubmit}>
              <Field label="Email address" required>
                <input
                  type="email"
                  required
                  value={loginForm.email}
                  onChange={(e) => setLoginForm((prev) => ({ ...prev, email: e.target.value }))}
                  placeholder={loginRoleTab === 'staff' ? 'staff@airport.test' : 'name@example.test'}
                />
              </Field>

              <Field label="Password" required>
                <input
                  type="password"
                  required
                  value={loginForm.password}
                  onChange={(e) => setLoginForm((prev) => ({ ...prev, password: e.target.value }))}
                  placeholder="Enter password"
                />
              </Field>

              <button type="submit" className="button button-primary button-full" disabled={isSubmitting}>
                {isSubmitting ? 'Signing in...' : (loginRoleTab === 'staff' ? 'Sign in as Staff' : 'Sign in')} <ArrowRight size={16} />
              </button>
            </form>
          </div>
        ) : (
          <form className="dialog-form" onSubmit={handleRegisterSubmit}>
            <Field label="Full name" required>
              <input
                type="text"
                required
                value={registerForm.fullName}
                onChange={(e) => setRegisterForm((prev) => ({ ...prev, fullName: e.target.value }))}
                placeholder="e.g. Airport Passenger"
              />
            </Field>

            <Field label="Email address" required>
              <input
                type="email"
                required
                value={registerForm.email}
                onChange={(e) => setRegisterForm((prev) => ({ ...prev, email: e.target.value }))}
                placeholder="passenger@airport.test"
              />
            </Field>

            <Field label="Phone number">
              <input
                type="tel"
                value={registerForm.phone}
                onChange={(e) => setRegisterForm((prev) => ({ ...prev, phone: e.target.value }))}
                placeholder="9000000001"
              />
            </Field>

            <Field label="Password (min 8 characters)" required>
              <input
                type="password"
                required
                minLength={8}
                value={registerForm.password}
                onChange={(e) => setRegisterForm((prev) => ({ ...prev, password: e.target.value }))}
                placeholder="Create password"
              />
            </Field>

            <button type="submit" className="button button-primary button-full" disabled={isSubmitting}>
              {isSubmitting ? 'Registering...' : 'Create account & sign in'} <ArrowRight size={16} />
            </button>
          </form>
        )}
      </Dialog>

      {/* ===================================================================
          MODAL 2: REPORT A MISSING ITEM (LIVE DB WRITE)
         =================================================================== */}
      <Dialog
        open={reportOpen}
        onOpenChange={setReportOpen}
        title="Report a missing item"
        description="Lodge details directly into the airport custody system. Required fields are marked with an asterisk."
        className="report-dialog"
      >
        <form className="dialog-form report-form" onSubmit={handleReportSubmit}>
          {reportError && (
            <div className="status-toast status-toast-error" role="alert">
              <CircleAlert size={18} />
              <span>{reportError}</span>
            </div>
          )}
          {reportSuccess && (
            <div className="status-toast status-toast-success" role="status">
              <CheckCircle2 size={18} />
              <span>{reportSuccess}</span>
            </div>
          )}

          <div className="form-section-label">
            <span>01</span>
            <strong>THE ITEM</strong>
          </div>

          <Field label="Item name" required>
            <input
              value={reportForm.itemName}
              onChange={(e) => setReportForm((prev) => ({ ...prev, itemName: e.target.value }))}
              placeholder="e.g. MacBook Pro 14 Inch"
              maxLength={150}
              required
            />
          </Field>

          <div className="form-grid-two">
            <Field label="Category" required>
              <select
                value={reportForm.categoryId}
                onChange={(e) => setReportForm((prev) => ({ ...prev, categoryId: Number(e.target.value) }))}
              >
                {categories.map((c) => (
                  <option key={c.categoryId} value={c.categoryId}>
                    {c.categoryName}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Condition">
              <select
                value={reportForm.itemCondition}
                onChange={(e) => setReportForm((prev) => ({ ...prev, itemCondition: e.target.value as any }))}
              >
                <option value="good">Good</option>
                <option value="new">New</option>
                <option value="fair">Fair</option>
                <option value="poor">Poor</option>
                <option value="damaged">Damaged</option>
                <option value="unknown">Unknown</option>
              </select>
            </Field>
          </div>

          <div className="form-grid-two">
            <Field label="Brand">
              <input
                value={reportForm.brand}
                onChange={(e) => setReportForm((prev) => ({ ...prev, brand: e.target.value }))}
                placeholder="e.g. Apple, Montblanc, Rimowa"
                maxLength={100}
              />
            </Field>
            <Field label="Colour">
              <input
                value={reportForm.colour}
                onChange={(e) => setReportForm((prev) => ({ ...prev, colour: e.target.value }))}
                placeholder="e.g. Space Gray, Deep Tan"
                maxLength={60}
              />
            </Field>
          </div>

          <Field label="Description">
            <textarea
              value={reportForm.description}
              onChange={(e) => setReportForm((prev) => ({ ...prev, description: e.target.value }))}
              placeholder="Distinctive stickers, engraving, scratch patterns, or serial numbers"
              rows={2}
              maxLength={4000}
            />
          </Field>

          <div className="form-section-label form-section-spaced">
            <span>02</span>
            <strong>WHEN &amp; WHERE WAS IT LOST</strong>
          </div>

          <div className="form-grid-two">
            <Field label="When was it lost?" required>
              <input
                type="datetime-local"
                value={reportForm.lostAt}
                onChange={(e) => setReportForm((prev) => ({ ...prev, lostAt: e.target.value }))}
                required
              />
            </Field>

            <Field label="Airport location">
              <select
                value={reportForm.locationId}
                onChange={(e) => setReportForm((prev) => ({ ...prev, locationId: Number(e.target.value) }))}
              >
                {locations.map((loc) => (
                  <option key={loc.locationId} value={loc.locationId}>
                    {loc.locationName}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="form-grid-two">
            <Field label="Flight (optional)">
              <select
                value={reportForm.flightId}
                onChange={(e) => setReportForm((prev) => ({ ...prev, flightId: e.target.value ? Number(e.target.value) : '' }))}
              >
                <option value="">No associated flight / Not applicable</option>
                {flights.map((f) => (
                  <option key={f.flightId} value={f.flightId}>
                    {f.airline} ({f.flightNumber})
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Estimated value (INR, optional)">
              <input
                type="number"
                min="0"
                step="0.01"
                value={reportForm.estimatedValue}
                onChange={(e) => setReportForm((prev) => ({ ...prev, estimatedValue: e.target.value }))}
                placeholder="e.g. 5000"
              />
            </Field>
          </div>

          <Field label="Additional notes (optional)">
            <textarea
              value={reportForm.additionalDetails}
              onChange={(e) => setReportForm((prev) => ({ ...prev, additionalDetails: e.target.value }))}
              placeholder="Seat number, gate zone, or any identifying marks"
              rows={2}
              maxLength={4000}
            />
          </Field>

          <div className="form-section-label form-section-spaced">
            <span>03</span>
            <strong>PHOTOS (OPTIONAL)</strong>
          </div>

          <ImageUploadWidget
            photos={reportForm.photos}
            onChange={(photos) => setReportForm((prev) => ({ ...prev, photos }))}
            maxPhotos={4}
            hint="Upload photos of the item or purchase receipts if available."
          />

          <div className="dialog-actions">
            <button
              type="button"
              className="button button-secondary"
              onClick={() => setReportOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              className="button button-primary"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Submitting...' : 'Submit Report'} <ArrowRight size={16} />
            </button>
          </div>

          <p className="dialog-footnote">
            <ShieldCheck size={15} /> Submitting as {currentUser?.fullName}.
          </p>
        </form>
      </Dialog>

      {/* ===================================================================
          MODAL 3: SERVICE INFORMATION
         =================================================================== */}
      <Dialog
        open={helpOpen}
        onOpenChange={setHelpOpen}
        title="How Lost & Found Works"
        description="A simple 4-step process to report and reclaim lost items."
        className="help-dialog"
      >
        <div className="help-content">
          <div className="notice notice-info">
            <ShieldCheck size={18} />
            <p>
              <strong>Airport Custody Network</strong>
              <span>
                Connected across security checkpoints, boarding gates, and lost property desks.
              </span>
            </p>
          </div>
          <div className="help-row">
            <span className="help-row-icon"><Search size={18} /></span>
            <div>
              <strong>1. Report Lost Item</strong>
              <p>Submit details and photos of what you lost.</p>
            </div>
          </div>
          <div className="help-row">
            <span className="help-row-icon"><Layers size={18} /></span>
            <div>
              <strong>2. Automatic Match</strong>
              <p>System alerts you when staff find a matching item.</p>
            </div>
          </div>
          <div className="help-row">
            <span className="help-row-icon"><MapPin size={18} /></span>
            <div>
              <strong>3. Verify Ownership</strong>
              <p>Verify your identity with your ID or boarding pass.</p>
            </div>
          </div>
          <div className="help-row">
            <span className="help-row-icon"><CheckCircle2 size={18} /></span>
            <div>
              <strong>4. Collect Item</strong>
              <p>Pick up your item from the airport counter or request delivery.</p>
            </div>
          </div>
          <button className="button button-primary button-full" onClick={() => setHelpOpen(false)}>
            Close <CheckCircle2 size={16} />
          </button>
        </div>
      </Dialog>

      {/* Lightbox for full-resolution photo viewing */}
      {lightboxImg && (
        <div className="lightbox-overlay" onClick={() => setLightboxImg(null)}>
          <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              className="lightbox-close-btn"
              onClick={() => setLightboxImg(null)}
              aria-label="Close"
            >
              <X size={20} />
            </button>
            <img src={lightboxImg} alt="Enlarged view" className="lightbox-img" />
          </div>
        </div>
      )}
    </div>
  );
}
