from fastapi import APIRouter, HTTPException
from Whispercpp.aly import analyze_debate

router = APIRouter(prefix="/analysis", tags=["Analysis"])


@router.post("/run")
def run_analysis():
    """
    Runs the debate analyzer (aly.py) and returns the result.
    """
    try:
        result = analyze_debate()
        return {
            "status": "success",
            "data": result
        }

    except FileNotFoundError:
        raise HTTPException(
            status_code=404,
            detail="debate_transcript.txt not found. Run STT first."
        )

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(e)
        )
