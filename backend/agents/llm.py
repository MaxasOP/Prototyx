"""Bounded provider calls; a Gemini outage must not disable Groq."""
import json
import os
import logging
import requests
from groq import Groq


def generate_text(prompt, json_mode=False):
    key = os.getenv('GEMINI_API_KEY')
    if key:
        try:
            model = os.getenv('GEMINI_MODEL', 'gemini-flash-latest')
            config = {'responseMimeType': 'application/json'} if json_mode else {}
            response = requests.post(
                f'https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent',
                headers={'x-goog-api-key': key},
                json={'contents': [{'parts': [{'text': prompt}]}], 'generationConfig': config}, timeout=35)
            response.raise_for_status()
            text = ''.join(p.get('text', '') for p in response.json()['candidates'][0]['content']['parts'])
            if not text.strip():
                raise ValueError('Empty Gemini response')
            if json_mode:
                json.loads(text)
            return text
        except (requests.RequestException, ValueError, KeyError, IndexError):
            pass
    key = os.getenv('GROQ_API_KEY')
    if not key:
        raise RuntimeError('AI provider unavailable. Configure GEMINI_API_KEY or GROQ_API_KEY.')
    models = list(dict.fromkeys([os.getenv('GROQ_MODEL', 'qwen/qwen3.8-27b'),
                                os.getenv('GROQ_FALLBACK_MODEL', 'openai/gpt-oss-120b')]))
    for model in models:
      try:
        kwargs = {'model': model,
                  'messages': [{'role': 'user', 'content': prompt}], 'max_tokens': 1200}
        if json_mode:
            kwargs['response_format'] = {'type': 'json_object'}
        result = Groq(api_key=key, timeout=35, max_retries=0).chat.completions.create(**kwargs)
        text = result.choices[0].message.content
        if not text or not text.strip():
            raise ValueError('Empty Groq response')
        if json_mode:
            json.loads(text)
        return text
      except Exception as exc:
        code = getattr(exc, 'status_code', None)
        logging.warning('AI generation failed: provider=Groq model=%s error=%s status=%s',
                        model, type(exc).__name__, code)
        if code in (401, 403):
            raise RuntimeError('AI provider credentials were rejected. Service configuration needs attention.') from exc
    raise RuntimeError('AI provider temporarily unavailable. Try again shortly.')
