# Footwork

Footwork is a mobile and web application that delivers **personalized soccer training plans** and a **player development roadmap** based on a user's position, skill level, and goals.

## Purpose of Amazon SES Usage

We use **Amazon Simple Email Service (SES)** exclusively for **transactional emails**, including:
- **Account verification emails** during signup
- **Password reset emails** upon user request

We **do not** send marketing or promotional emails through SES.  
All recipients are registered users who have explicitly created accounts on our platform.

## Email Sending Setup
- **Region:** `us-east-2`
- **Domain:** `getfootwork.app` (DKIM, SPF, and DMARC configured)
- **From Address:** `no-reply@getfootwork.app`

## Application Features
- Drill library with filters by position, category, and difficulty
- Daily training plans generated based on player profile
- Player dashboard with progress tracking
- Roadmap/level system with milestones
- Session logging

## Status
Footwork is currently in **pre-launch development**.  
A live demo will be available after backend deployment to AWS EC2 and database migration to AWS RDS.
