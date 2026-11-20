import { useEffect, useState } from 'react'
import './App.css'

import Header from './components/Header'
import UploadMeeting from './components/UploadMeeting'
import MeetingHistory from './components/MeetingHistory'
import MeetingDetails from './components/MeetingDetails'

import {
    getAllMeetings,
    uploadMeeting,
    getMeetingStatus
} from './services/meetingApi'


function App()
{
    const [meetings, setMeetings] = useState([])

    const [selectedMeeting, setSelectedMeeting] =
        useState(null)

    const [loading, setLoading] =
        useState(false)

    const [processingMeeting, setProcessingMeeting] = useState(null)

    const [message, setMessage] =
        useState('')

    const [messageType, setMessageType] =
        useState('info')


    useEffect(() =>
    {
        loadMeetings()
    }, [])


    const loadMeetings = async () =>
    {
        try
        {
            const data =
                await getAllMeetings()


            const sortedMeetings =
                [...data].sort(
                    (firstMeeting, secondMeeting) =>
                        secondMeeting.id - firstMeeting.id
                )


            setMeetings(
                sortedMeetings
            )


            setSelectedMeeting(
                (currentMeeting) =>
                {
                    if (currentMeeting)
                    {
                        return currentMeeting
                    }

                    return sortedMeetings[0] || null
                }
            )
        }
        catch (error)
        {
            console.error(error)

            setMessage(
                'Unable to connect to the MeetMind backend.'
            )

            setMessageType(
                'error'
            )
        }
    }


    const handleUpload = async (title, file) =>
    {
        if (!title.trim() || !file) {
            setMessage(!title.trim() ? 'Please enter a meeting title.' : 'Please select an audio or video file.')
            setMessageType('error')
            return false
        }

        try {
            setLoading(true)
            setMessage('Uploading your meeting recording...')
            setMessageType('processing')

            let current = await uploadMeeting(title.trim(), file)
            setProcessingMeeting(current)
            setSelectedMeeting(current)
            setMessage('Upload complete. Processing has started.')

            const terminalStatuses = ['COMPLETED', 'AI_PROCESSING_FAILED', 'FAILED']
            while (!terminalStatuses.includes((current.status || '').toUpperCase())) {
                await new Promise(resolve => setTimeout(resolve, 1500))
                current = await getMeetingStatus(current.id)
                setProcessingMeeting(current)
                setSelectedMeeting(current)
                setMeetings(previous => {
                    const withoutCurrent = previous.filter(item => item.id !== current.id)
                    return [current, ...withoutCurrent]
                })
                const stage = (current.processingStage || current.status || 'PROCESSING').replaceAll('_', ' ').toLowerCase()
                setMessage(`MeetMind is processing: ${stage} (${current.progressPercent ?? 0}%)`)
            }

            setProcessingMeeting(current)
            setSelectedMeeting(current)
            await loadMeetings()
            setSelectedMeeting(current)

            if (current.status === 'COMPLETED') {
                setMessage('Meeting processed successfully. Your AI insights are ready.')
                setMessageType('success')
            } else {
                setMessage(current.processingError || 'Meeting processing failed. Check that the AI service is running.')
                setMessageType('error')
            }

            setTimeout(() => {
                document.getElementById('meeting-details')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
            }, 100)
            return current.status === 'COMPLETED'
        } catch (error) {
            console.error(error)
            setMessage(error.message || 'Unable to process the meeting.')
            setMessageType('error')
            return false
        } finally {
            setLoading(false)
        }
    }

    const handleSelectMeeting =
        (meeting) =>
    {
        setSelectedMeeting(
            meeting
        )


        setTimeout(() =>
        {
            document
                .getElementById(
                    'meeting-details'
                )
                ?.scrollIntoView(
                    {
                        behavior: 'smooth',
                        block: 'start'
                    }
                )
        }, 100)
    }


    return (
        <div className="app">

            <Header />


            <main className="main-container">


                <section className="hero-section">

                    <div className="hero-content">

                        <span className="hero-badge">
                            AI MEETING INTELLIGENCE
                        </span>


                        <h1>
                            Turn every meeting into
                            <span>
                                {' '}
                                clear insights.
                            </span>
                        </h1>


                        <p>
                            Upload your meeting recording and let
                            MeetMind AI generate an accurate transcript,
                            concise summary, key decisions, and
                            actionable follow-up tasks.
                        </p>


                        <div className="hero-features">

                            <span>
                                ✦ AI Summaries
                            </span>

                            <span>
                                ◉ Smart Transcription
                            </span>

                            <span>
                                ✓ Action Items
                            </span>

                        </div>

                    </div>

                </section>


                <UploadMeeting
                    loading={loading}
                    onUpload={handleUpload}
                />


                {
                    message &&
                    (
                        <div
                            className={
                                `message message-${messageType}`
                            }
                        >
                            {
                                loading &&
                                (
                                    <span className="button-spinner"></span>
                                )
                            }

                            <span>
                                {message}
                            </span>
                        </div>
                    )
                }

                {
                    processingMeeting && loading &&
                    (
                        <section className="pipeline-progress" aria-live="polite">
                            <div className="pipeline-progress-heading">
                                <strong>Meeting processing pipeline</strong>
                                <span>{processingMeeting.progressPercent ?? 0}%</span>
                            </div>
                            <div className="pipeline-progress-track">
                                <div className="pipeline-progress-fill" style={{ width: `${processingMeeting.progressPercent ?? 0}%` }} />
                            </div>
                            <p>{(processingMeeting.processingStage || processingMeeting.status || 'QUEUED').replaceAll('_', ' ')}</p>
                            <div className="pipeline-stage-list">
                                {['QUEUED', 'TRANSCRIPTION', 'AI_ANALYSIS', 'SAVING_RESULTS', 'COMPLETED'].map((stage, index) => {
                                    const order = ['QUEUED', 'TRANSCRIPTION', 'AI_ANALYSIS', 'SAVING_RESULTS', 'COMPLETED']
                                    const currentIndex = order.indexOf(processingMeeting.processingStage)
                                    const done = currentIndex >= index || processingMeeting.status === 'COMPLETED'
                                    return <span key={stage} className={done ? 'pipeline-stage done' : 'pipeline-stage'}>{stage.replaceAll('_', ' ')}</span>
                                })}
                            </div>
                        </section>
                    )
                }


                <div className="workspace-grid">


                    <MeetingHistory
                        meetings={meetings}
                        selectedMeeting={
                            selectedMeeting
                        }
                        onSelectMeeting={
                            handleSelectMeeting
                        }
                        loading={loading}
                    />


                    <div id="meeting-details">

                        <MeetingDetails
                            meeting={
                                selectedMeeting
                            }
                        />

                    </div>


                </div>


            </main>


            <footer className="footer">

    <div className="footer-content">

        <div>

            <strong>
                MeetMind AI
            </strong>

            <p>
                Intelligent meeting transcription,
                summarization, decisions,
                and action-item extraction.
            </p>

        </div>

       <div className="footer-credit">

    <span>
        Designed & Developed by
    </span>

    <strong>
        Jyothisri
    </strong>

    <span className="footer-divider">
        •
    </span>

    <span>
        Faster-Whisper • Qwen3 • Spring Boot • React
    </span>

</div>

    </div>

</footer>

        </div>
    )
}


export default App