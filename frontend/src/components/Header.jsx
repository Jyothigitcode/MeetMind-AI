function Header()
{
    return (
        <header className="top-header">
            <div className="header-content">
                <p className="header-eyebrow">
                    AI Meeting Workspace
                </p>

                <h1>Welcome to MeetMind</h1>

                <p className="header-description">
                    Transform your meetings into searchable transcripts
                    and intelligent AI summaries.
                </p>
            </div>

            <div className="header-actions">
                <div className="header-status">
                    <span className="header-status-dot"></span>
                    <span>AI Ready</span>
                </div>

                <button
                    className="new-meeting-button"
                    type="button"
                >
                    <span>＋</span>
                    New Meeting
                </button>
            </div>
        </header>
    )
}

export default Header