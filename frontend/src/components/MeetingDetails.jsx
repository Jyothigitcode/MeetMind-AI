import {
    CalendarDays,
    FileText,
    Flag,
    ListTodo,
    Sparkles,
    UserRound
} from 'lucide-react'


function parseArray(value)
{
    if (!value)
    {
        return []
    }

    if (Array.isArray(value))
    {
        return value
    }

    try
    {
        const parsedValue = JSON.parse(value)

        return Array.isArray(parsedValue)
            ? parsedValue
            : []
    }
    catch
    {
        return []
    }
}


function getStatusText(status)
{
    if (!status)
    {
        return 'Unknown'
    }

    return status
        .replaceAll('_', ' ')
        .toLowerCase()
        .replace(/\b\w/g, (letter) => letter.toUpperCase())
}


function MeetingDetails({ meeting })
{
    if (!meeting)
    {
        return (
            <section className="details-card empty-details">

                <div className="empty-details-icon">
                    <Sparkles size={22} />
                </div>

                <span className="section-label">
                    MEETING INSIGHTS
                </span>

                <h2>Select a meeting</h2>

                <p>
                    Choose a processed meeting from your library
                    to view its AI-generated insights, decisions,
                    action items, and complete transcript.
                </p>

            </section>
        )
    }


    const decisions = parseArray(meeting.keyDecisions)
    const actionItems = parseArray(meeting.actionItems)
    const transcriptSegments = parseArray(meeting.transcriptSegments)

    const formatTimestamp = (seconds) => {
        const value = Math.max(0, Math.floor(Number(seconds) || 0))
        return String(Math.floor(value / 60)).padStart(2, "0") + ":" + String(value % 60).padStart(2, "0")
    }


    return (
        <section className="details-card">

            <div className="details-header">

                <div>

                    <span className="section-label">
                        MEETING INSIGHTS
                    </span>

                    <h2>
                        {meeting.title || 'Untitled Meeting'}
                    </h2>

                    <p>
                        AI-generated summary, key decisions,
                        action items, and complete transcription.
                    </p>

                </div>


                <span
                    className={
                        `meeting-status status-${meeting.status?.toLowerCase()}`
                    }
                >
                    {getStatusText(meeting.status)}
                </span>

            </div>


            <div className="processing-metrics">
                <div><span>Transcription</span><strong>{meeting.transcriptionSeconds != null ? Number(meeting.transcriptionSeconds).toFixed(1) + "s" : "Not recorded"}</strong></div>
                <div><span>AI analysis</span><strong>{meeting.analysisSeconds != null ? Number(meeting.analysisSeconds).toFixed(1) + "s" : "Not recorded"}</strong></div>
                <div><span>Pipeline stage</span><strong>{(meeting.processingStage || meeting.status || "Unknown").replaceAll("_", " ")}</strong></div>
            </div>

            <div className="insight-grid">

                <article className="insight-panel summary-panel">

                    <div className="panel-heading">

                        <div className="panel-icon">
                            <Sparkles size={18} />
                        </div>

                        <div>
                            <span className="panel-label">
                                AI GENERATED
                            </span>

                            <h3>
                                Meeting Summary
                            </h3>
                        </div>

                    </div>


                    <div className="panel-content">

                        {
                            meeting.summary
                                ?
                                (
                                    <p className="summary-text">
                                        {meeting.summary}
                                    </p>
                                )
                                :
                                (
                                    <p className="missing-content">
                                        Summary is not available for this meeting.
                                    </p>
                                )
                        }

                    </div>

                </article>


                <div className="outcomes-grid">

                    <article className="insight-panel decisions-panel">

                        <div className="panel-heading">

                            <div className="panel-icon">
                                <Flag size={18} />
                            </div>

                            <div>
                                <span className="panel-label">
                                    AGREED OUTCOMES
                                </span>

                                <h3>
                                    Key Decisions
                                </h3>
                            </div>

                        </div>


                        <div className="panel-content">

                            {
                                decisions.length > 0
                                    ?
                                    (
                                        <ul className="decision-list">
                                            {
                                                decisions.map(
                                                    (decision, index) =>
                                                    (
                                                        <li key={`${decision}-${index}`}>
                                                            {decision}
                                                        </li>
                                                    )
                                                )
                                            }
                                        </ul>
                                    )
                                    :
                                    (
                                        <p className="missing-content">
                                            No explicit decisions were identified.
                                        </p>
                                    )
                            }

                        </div>

                    </article>


                    <article className="insight-panel action-items-panel">

                        <div className="panel-heading">

                            <div className="panel-icon">
                                <ListTodo size={18} />
                            </div>

                            <div>
                                <span className="panel-label">
                                    FOLLOW-UP WORK
                                </span>

                                <h3>
                                    Action Items
                                </h3>
                            </div>

                        </div>


                        <div className="panel-content">

                            {
                                actionItems.length > 0
                                    ?
                                    (
                                        <ul className="action-list">
                                            {
                                                actionItems.map(
                                                    (item, index) =>
                                                    (
                                                        <li
                                                            className="action-item"
                                                            key={`${item.task}-${index}`}
                                                        >
                                                            <div className="action-item-content">
                                                                <strong>
                                                                    {item.task}
                                                                </strong>

                                                                <div className="action-item-meta">
                                                                    <span>
                                                                        <UserRound size={12} />
                                                                        {item.owner || 'Unassigned'}
                                                                    </span>

                                                                    {
                                                                        item.dueDate &&
                                                                        (
                                                                            <span>
                                                                                <CalendarDays size={12} />
                                                                                Due {item.dueDate}
                                                                            </span>
                                                                        )
                                                                    }
                                                                </div>
                                                            </div>

                                                            <span
                                                                className={
                                                                    `priority-badge priority-${
                                                                        (item.priority || 'MEDIUM').toLowerCase()
                                                                    }`
                                                                }
                                                            >
                                                                {item.priority || 'MEDIUM'}
                                                            </span>
                                                        </li>
                                                    )
                                                )
                                            }
                                        </ul>
                                    )
                                    :
                                    (
                                        <p className="missing-content">
                                            No explicit action items were identified.
                                        </p>
                                    )
                            }

                        </div>

                    </article>

                </div>


                <article className="insight-panel transcript-panel">

                    <div className="panel-heading">

                        <div className="panel-icon">
                            <FileText size={18} />
                        </div>

                        <div>
                            <span className="panel-label">
                                FULL RECORD
                            </span>

                            <h3>
                                Transcript
                            </h3>
                        </div>

                    </div>


                    <div className="panel-content transcript-content">

                        {
                            meeting.transcript
                                ?
                                (
                                    transcriptSegments.length > 0
                                        ? (
                                            <div className="timestamped-transcript">
                                                {transcriptSegments.map((segment, index) => (
                                                    <div className="transcript-segment" key={String(segment.start) + "-" + index}>
                                                        <span className="transcript-time">{formatTimestamp(segment.start)}</span>
                                                        <p>{segment.text}</p>
                                                    </div>
                                                ))}
                                            </div>
                                        )
                                        : <p className="transcript-text">{meeting.transcript}</p>
                                )
                                :
                                (
                                    <p className="missing-content">
                                        Transcript is not available for this meeting.
                                    </p>
                                )
                        }

                    </div>

                </article>

            </div>

        </section>
    )
}


export default MeetingDetails
