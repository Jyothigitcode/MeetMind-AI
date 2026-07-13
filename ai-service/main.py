import shutil
import tempfile
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile
from faster_whisper import WhisperModel
from meeting_analysis import extract_action_items, extract_key_decisions, split_sentences
from pydantic import BaseModel
from transformers import pipeline


app = FastAPI(
    title="MeetMind AI Service",
    description="Local AI transcription and meeting analysis service for MeetMind",
    version="1.1.0",
)


whisper_model = WhisperModel(
    "base",
    device="cpu",
    compute_type="int8",
)


summarizer = pipeline(
    "summarization",
    model="sshleifer/distilbart-cnn-12-6",
)


class SummarizeRequest(BaseModel):
    transcript: str


@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "MeetMind AI Service",
    }


def split_transcript_into_chunks(transcript: str, max_characters: int = 3000) -> list[str]:
    chunks: list[str] = []
    current_chunk = ""

    for sentence in split_sentences(transcript):
        if len(sentence) > max_characters:
            if current_chunk:
                chunks.append(current_chunk)
                current_chunk = ""

            chunks.extend(
                sentence[index:index + max_characters]
                for index in range(0, len(sentence), max_characters)
            )
            continue

        candidate = f"{current_chunk} {sentence}".strip()

        if current_chunk and len(candidate) > max_characters:
            chunks.append(current_chunk)
            current_chunk = sentence
        else:
            current_chunk = candidate

    if current_chunk:
        chunks.append(current_chunk)

    return chunks


def generate_summary(transcript: str) -> str:
    words = transcript.split()

    if len(words) < 45:
        return transcript

    partial_summaries: list[str] = []

    for chunk in split_transcript_into_chunks(transcript):
        word_count = len(chunk.split())
        max_length = min(150, max(40, word_count // 2))
        min_length = min(30, max_length - 5)

        result = summarizer(
            chunk,
            max_length=max_length,
            min_length=min_length,
            do_sample=False,
        )

        partial_summaries.append(result[0]["summary_text"])

    return " ".join(partial_summaries)


@app.post("/transcribe")
def transcribe(file: UploadFile = File(...)):
    temp_path: str | None = None

    try:
        suffix = Path(file.filename or "meeting-audio").suffix

        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
            shutil.copyfileobj(file.file, temp_file)
            temp_path = temp_file.name

        segments, info = whisper_model.transcribe(
            temp_path,
            beam_size=5,
            vad_filter=True,
        )

        transcript = " ".join(
            segment.text.strip()
            for segment in segments
            if segment.text.strip()
        )

        return {
            "transcript": transcript,
            "language": info.language,
            "languageProbability": info.language_probability,
            "duration": info.duration,
        }

    except Exception as exception:
        raise HTTPException(
            status_code=500,
            detail=f"Transcription failed: {str(exception)}",
        ) from exception

    finally:
        file.file.close()

        if temp_path:
            Path(temp_path).unlink(missing_ok=True)


@app.post("/summarize")
def summarize(request: SummarizeRequest):
    try:
        transcript = request.transcript.strip()

        if not transcript:
            raise HTTPException(
                status_code=400,
                detail="Transcript cannot be empty",
            )

        return {
            "summary": generate_summary(transcript),
            "keyDecisions": extract_key_decisions(transcript),
            "actionItems": extract_action_items(transcript),
        }

    except HTTPException:
        raise

    except Exception as exception:
        raise HTTPException(
            status_code=500,
            detail=f"Meeting analysis failed: {str(exception)}",
        ) from exception
