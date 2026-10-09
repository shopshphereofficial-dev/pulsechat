package com.pulsechat.app.data

object Supabase {
    const val URL = "https://vildgvfcyhawojpmtdbe.supabase.co"

    // Public "anon" key. This is designed to be shipped inside a client app.
    // It is NOT a secret: it only grants what your Row Level Security policies allow.
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZpbGRndmZjeWhhd29qcG10ZGJlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE1MTI1NjksImV4cCI6MjEwNzA4ODU2OX0.0iodEfFw7Llx6kgsklXfV9gsRlpsrPe6kNR72kHKs4Y"

    // TODO(user): paste your Google OAuth *Web* client ID here (see README for setup).
    const val GOOGLE_WEB_CLIENT_ID = "PUT_YOUR_GOOGLE_WEB_CLIENT_ID_HERE"
}
