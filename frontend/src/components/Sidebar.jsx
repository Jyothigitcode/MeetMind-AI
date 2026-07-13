function Sidebar()
{
    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <div className="brand-icon">
                    M
                </div>

                <div className="brand-text">
                    <h2>MeetMind</h2>
                    <span>AI Workspace</span>
                </div>
            </div>


            <nav className="sidebar-navigation">

                <button className="nav-item active">
                    <span className="nav-icon">⌂</span>
                    <span>Dashboard</span>
                </button>

                <button className="nav-item">
                    <span className="nav-icon">＋</span>
                    <span>New Meeting</span>
                </button>

                <button className="nav-item">
                    <span className="nav-icon">◷</span>
                    <span>Meeting History</span>
                </button>

            </nav>


            <div className="sidebar-bottom">

                <div className="ai-status">
                    <div className="status-indicator"></div>

                    <div>
                        <p>AI Services</p>
                        <span>System operational</span>
                    </div>
                </div>


                <div className="sidebar-footer">
                    <div className="user-avatar">
                        V
                    </div>

                    <div className="user-information">
                        <p>MeetMind User</p>
                        <span>Workspace</span>
                    </div>
                </div>

            </div>
        </aside>
    )
}

export default Sidebar