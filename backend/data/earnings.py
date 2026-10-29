import os
from urllib.parse import quote
import requests

BASE_URL = 'https://api.roic.ai/v3.0.0'

def _get(path, params=None):
    key = os.getenv('ROIC_API_KEY')
    if not key:
        raise RuntimeError('Set ROIC_API_KEY in the backend environment')
    response = requests.get(BASE_URL + path, params=params,
                            headers={'Authorization': 'Bearer ' + key}, timeout=15)
    if response.status_code in (401, 403):
        raise RuntimeError('ROIC credentials were rejected')
    if response.status_code == 402:
        raise RuntimeError('This transcript is outside the ROIC plan entitlement')
    if not response.ok:
        raise RuntimeError('ROIC request failed (HTTP %s)' % response.status_code)
    return response.json()

def get_earnings_transcript(ticker, year=2026, quarter=3):
    if not ticker.strip() or quarter not in range(1,5):
        raise ValueError('Provide a ticker and quarter from 1 to 4')
    identifier = ticker.strip().upper()
    # Bare symbols can be ambiguous across exchanges; only resolve known listings.
    listings = {'AAPL':'NASDAQ:AAPL', 'MSFT':'NASDAQ:MSFT', 'NVDA':'NASDAQ:NVDA',
                'TCS':'NSE:TCS', 'RELIANCE':'NSE:RELIANCE', 'INFY':'NSE:INFY'}
    identifier = listings.get(identifier, identifier)
    if ':' not in identifier:
        raise ValueError('Use an exchange-qualified ticker, for example NASDAQ:AAPL')
    page = _get('/company-events', {'identifier':identifier, 'content':'transcript', 'limit':100})
    for _ in range(20):
        for event in page.get('data', []):
            if event.get('fiscal_year') != year or event.get('fiscal_quarter') != quarter:
                continue
            raw = next((t for t in event.get('transcripts', []) if t.get('type') == 'raw'), None)
            if not raw:
                continue
            document = _get('/documents/transcripts/' + quote(raw['id'], safe='') + '/raw')
            text = document.get('text')
            if not isinstance(text, str) or not text.strip():
                raise ValueError('ROIC returned an empty transcript')
            return {'ticker':identifier, 'company_name':identifier,
                    'quarter':f'Q{quarter} {year}', 'prepared_remarks':text,
                    'qa_session':'Included in the full raw transcript above; speaker labels are unavailable.',
                    'source':'ROIC.ai', 'asOf':document.get('updated_at')}
        next_url = page.get('next_page_url')
        if not next_url:
            break
        # Follow only provider-relative pagination, never an arbitrary host.
        if not next_url.startswith('/v3.0.0/company-events?'):
            raise RuntimeError('Unexpected ROIC pagination response')
        page = _get(next_url[len('/v3.0.0'):])
    raise ValueError('No accessible transcript for this fiscal period; check company coverage and plan history')
