import { useRef, useState } from 'react'

function UploadMeeting({ onUpload, loading })
{
    const [title, setTitle] = useState('')
    const [file, setFile] = useState(null)
    const [dragActive, setDragActive] = useState(false)

    const fileInputRef = useRef(null)


    const handleFileSelection = (selectedFile) =>
    {
        if (selectedFile)
        {
            setFile(selectedFile)
        }
    }


    const handleFileChange = (event) =>
    {
        handleFileSelection(event.target.files[0])
    }


    const handleDragOver = (event) =>
    {
        event.preventDefault()

        setDragActive(true)
    }


    const handleDragLeave = (event) =>
    {
        event.preventDefault()

        setDragActive(false)
    }


    const handleDrop = (event) =>
    {
        event.preventDefault()

        setDragActive(false)

        const droppedFile = event.dataTransfer.files[0]

        handleFileSelection(droppedFile)
    }


    const handleSubmit = async (event) =>
    {
        event.preventDefault()

        if (!title.trim() || !file)
        {
            return
        }

        const success = await onUpload(title, file)

        if (success)
        {
            setTitle('')
            setFile(null)

            if (fileInputRef.current)
            {
                fileInputRef.current.value = ''
            }
        }
    }


    const removeSelectedFile = () =>
    {
        setFile(null)

        if (fileInputRef.current)
        {
            fileInputRef.current.value = ''
        }
    }


    const formatFileSize = (bytes) =>
    {
        if (bytes === 0)
        {
            return '0 Bytes'
        }

        const sizeUnits =
            [
                'Bytes',
                'KB',
                'MB',
                'GB'
            ]

        const unitIndex =
            Math.floor(
                Math.log(bytes) /
                Math.log(1024)
            )

        const fileSize =
            bytes /
            Math.pow(1024, unitIndex)

        return `${fileSize.toFixed(1)} ${sizeUnits[unitIndex]}`
    }


    return (
        <section
            className="upload-card"
            id="new-meeting"
        >
            <div className="section-heading">
                <div>
                    <span className="section-label">
                        NEW ANALYSIS
                    </span>

                    <h2>Upload a meeting</h2>

                    <p>
                        Add your recording and let MeetMind
                        generate the transcript and AI summary.
                    </p>
                </div>

                <div className="secure-processing">
                    <span>●</span>
                    Secure processing
                </div>
            </div>


            <form onSubmit={handleSubmit}>

                <div className="meeting-title-field">
                    <label htmlFor="meeting-title">
                        Meeting title
                    </label>

                    <input
                        id="meeting-title"
                        type="text"
                        placeholder="Example: Weekly product sync"
                        value={title}
                        disabled={loading}
                        onChange={
                            (event) =>
                                setTitle(event.target.value)
                        }
                    />
                </div>


                <div
                    className={
                        dragActive
                            ? 'upload-dropzone drag-active'
                            : 'upload-dropzone'
                    }
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    onDrop={handleDrop}
                    onClick={
                        () =>
                        {
                            if (!loading)
                            {
                                fileInputRef.current?.click()
                            }
                        }
                    }
                >
                    <input
                        ref={fileInputRef}
                        className="hidden-file-input"
                        type="file"
                        accept="audio/*,video/*"
                        disabled={loading}
                        onChange={handleFileChange}
                    />


                    {
                        file
                            ?
                            (
                                <div className="selected-file">

                                    <div className="selected-file-icon">
                                        ♪
                                    </div>

                                    <div className="selected-file-information">
                                        <strong>
                                            {file.name}
                                        </strong>

                                        <span>
                                            {formatFileSize(file.size)}
                                        </span>
                                    </div>

                                    <button
                                        className="remove-file-button"
                                        type="button"
                                        disabled={loading}
                                        onClick={
                                            (event) =>
                                            {
                                                event.stopPropagation()

                                                removeSelectedFile()
                                            }
                                        }
                                    >
                                        ×
                                    </button>

                                </div>
                            )
                            :
                            (
                                <div className="empty-dropzone">

                                    <div className="upload-icon">
                                        ↑
                                    </div>

                                    <h3>
                                        Drop your meeting recording here
                                    </h3>

                                    <p>
                                        or click to browse from your computer
                                    </p>

                                    <span>
                                        Supports audio and video files up to 100 MB
                                    </span>

                                </div>
                            )
                    }

                </div>


                <div className="upload-footer">

                    <p>
                        Your recording will be processed by
                        MeetMind's AI pipeline.
                    </p>

                    <button
                        className="process-meeting-button"
                        type="submit"
                        disabled={
                            loading ||
                            !title.trim() ||
                            !file
                        }
                    >
                        {
                            loading
                                ?
                                (
                                    <>
                                        <span className="button-spinner"></span>
                                        Processing meeting...
                                    </>
                                )
                                :
                                (
                                    <>
                                        <span>✦</span>
                                        Generate AI insights
                                    </>
                                )
                        }
                    </button>

                </div>

            </form>
        </section>
    )
}

export default UploadMeeting