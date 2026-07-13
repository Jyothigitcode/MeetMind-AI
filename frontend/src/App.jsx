import { useEffect, useState } from 'react'
import './App.css'

import Header from './components/Header'
import UploadMeeting from './components/UploadMeeting'
import MeetingHistory from './components/MeetingHistory'
import MeetingDetails from './components/MeetingDetails'


function App()
{
    const [meetings, setMeetings] = useState([])

    const [selectedMeeting, setSelectedMeeting] =
        useState(null)

    const [loading, setLoading] = useState(false)

    const [message, setMessage] = useState('')


    const API_URL =
        'http://localhost:8080/api/meetings'


    useEffect(() =>
    {
        fetchMeetings()
    }, [])


    const fetchMeetings = async () =>
    {
        try
        {
            const response =
                await fetch(API_URL)


            if (!response.ok)
            {
                throw new Error(
                    'Unable to load meetings'
                )
            }


            const data =
                await response.json()


            setMeetings(data)
        }
        catch (error)
        {
            console.error(error)

            setMessage(
                'Unable to connect to backend'
            )
        }
    }


    const handleUpload = async (title, file) =>
    {
        if (!title.trim())
        {
            setMessage(
                'Please enter a meeting title'
            )

            return false
        }


        if (!file)
        {
            setMessage(
                'Please select an audio or video file'
            )

            return false
        }


        try
        {
            setLoading(true)

            setMessage(
                'Processing your meeting...'
            )


            const formData =
                new FormData()


            formData.append(
                'title',
                title.trim()
            )


            formData.append(
                'file',
                file
            )


            const response =
                await fetch(
                    `${API_URL}/upload`,
                    {
                        method: 'POST',
                        body: formData
                    }
                )


            if (!response.ok)
            {
                throw new Error(
                    'Meeting processing failed'
                )
            }


            const processedMeeting =
                await response.json()


            setSelectedMeeting(
                processedMeeting
            )


            setMessage(
                'Meeting processed successfully'
            )


            await fetchMeetings()


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


            return true
        }
        catch (error)
        {
            console.error(error)

            setMessage(
                'Unable to process meeting'
            )


            return false
        }
        finally
        {
            setLoading(false)
        }
    }


    const handleSelectMeeting = (meeting) =>
    {
        setSelectedMeeting(meeting)


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
                            Upload your meeting recording
                            and let MeetMind AI automatically
                            generate an accurate transcript
                            and intelligent summary.
                        </p>


                        <div className="hero-features">

                            <span>
                                ✦ AI Summaries
                            </span>

                            <span>
                                ◉ Smart Transcription
                            </span>

                            <span>
                                ✓ Meeting History
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
                        <p className="message">
                            {message}
                        </p>
                    )
                }


                <div className="workspace-grid">


                    <MeetingHistory
                        meetings={meetings}
                        selectedMeeting={selectedMeeting}
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
                            Intelligent meeting
                            transcription and summarization.
                        </p>

                    </div>


                    <span>
                        Powered by AI
                    </span>

                </div>

            </footer>

        </div>
    )
}


export default App