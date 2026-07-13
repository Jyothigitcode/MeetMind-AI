const API_BASE_URL = 'http://localhost:8080/api/meetings'


export async function getAllMeetings()
{
    const response = await fetch(API_BASE_URL)

    if (!response.ok)
    {
        throw new Error('Failed to fetch meetings')
    }

    return response.json()
}


export async function uploadMeeting(title, file)
{
    const formData = new FormData()

    formData.append('title', title)
    formData.append('file', file)

    const response = await fetch(
        `${API_BASE_URL}/upload`,
        {
            method: 'POST',
            body: formData
        }
    )

    if (!response.ok)
    {
        let errorMessage = 'Failed to process meeting'

        try
        {
            const errorData = await response.json()

            errorMessage =
                errorData.message ||
                errorData.error ||
                errorMessage
        }
        catch
        {
            // Keep default error message
        }

        throw new Error(errorMessage)
    }

    return response.json()
}


export async function deleteMeeting(id)
{
    const response = await fetch(
        `${API_BASE_URL}/${id}`,
        {
            method: 'DELETE'
        }
    )

    if (!response.ok)
    {
        throw new Error('Failed to delete meeting')
    }
}