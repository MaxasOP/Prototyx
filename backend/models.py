from typing import List, Optional, Dict, Any
from sqlmodel import SQLModel, Field, Relationship
from pydantic import BaseModel, field_validator
import math

# --- Database Models ---

class User(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    email: str = Field(index=True, unique=True)
    hashed_password: str
    name: Optional[str] = None
    
    holdings: List["Holding"] = Relationship(back_populates="user")

class Holding(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    ticker: str = Field(index=True)
    weight: float
    
    user_id: int = Field(foreign_key="user.id")
    user: User = Relationship(back_populates="holdings")

# --- API Request/Response Models (Matching Android App) ---

class LoginRequest(BaseModel):
    email: str
    password: str

class RegisterRequest(BaseModel):
    email: str
    password: str
    name: Optional[str] = None
    @field_validator('email')
    @classmethod
    def valid_email(cls, value):
        value = value.strip().lower()
        if '@' not in value or len(value)>254:
            raise ValueError('Enter a valid email address')
        return value
    @field_validator('password')
    @classmethod
    def valid_password(cls, value):
        if len(value)<8 or len(value.encode('utf-8'))>72:
            raise ValueError('Password must contain at least 8 characters and at most 72 UTF-8 bytes')
        return value

class AuthResponse(BaseModel):
    accessToken: str
    userEmail: str

class SyncHoldingsRequest(BaseModel):
    holdings: Dict[str, float]

class TickerSearchResponse(BaseModel):
    ticker: str
    name: str
    sector: Optional[str] = None

class MarketIndicatorsResponse(BaseModel):
    ticker: str
    close: float
    sma50: Optional[float] = None
    sma200: Optional[float] = None
    rsi: Optional[float] = None
    peRatio: Optional[float] = None
    dividendYield: Optional[float] = None
    aiMemo: Optional[str] = None

class EarningsTranscriptResponse(BaseModel):
    ticker: str
    transcript: str

class RiskMeshRequest(BaseModel):
    tickers: List[str]
    weights: List[float]

class RiskMeshResponse(BaseModel):
    mesh: Dict[str, Any] # Generic for now

class OptimizeRequest(BaseModel):
    tickers: List[str]
    views: Optional[Dict[str, float]] = None

class OptimizeResponse(BaseModel):
    optimized_weights: Dict[str, float]

class TickerListRequest(BaseModel):
    tickers: List[str]

class DebateResponse(BaseModel):
    debate_transcript: str

class ConsultRequest(BaseModel):
    query: str

class ConsultResponse(BaseModel):
    answer: str
