## Mind2Career
# Overview

Mind2Career is a rule based career recommendation system designed to help users discover suitable career paths using multiple data points. It combines personality test results, academic performance, and extracted resume skills to generate personalized career suggestions.

The system also provides career insights, peer comparison, and career switching guidance to support better decision making.

# Core Features
1. Personality Assessment

Users complete a structured personality test.
Results are used to understand interests, behavior patterns, and preferences.

2. Academic Analysis

Users enter their academic grades.
The system maps grades to relevant subjects and fields.

3. Resume Skill Extraction

Users upload their resume.
The system extracts key skills from the resume text.
These skills are used in the recommendation process.

4. Rule Based Recommendation Engine

Career suggestions are generated using predefined rules.
Inputs used:
• Personality type
• Academic grades
• Extracted skills from resume

Final output is a ranked list of suitable career paths.

5. Insights Module

Stores all recommended careers.
Users can view past recommendations anytime.
Helps track changes and progress over time.

6. Peer Comparison

Users can compare their profile with peers at the same academic level.
Helps users understand their relative strengths and positioning.

7. Career Switching Support

Provides basic information about alternative careers.
Includes required skills, learning paths, and external resources.

# Tech Stack
Android Development

• Kotlin
• XML (UI Design)

Backend / Database

• Firebase (Realtime Database / Firestore)

Recommendation System

• Rule based logic (no machine learning model)

# How It Works
User registers and completes personality test
User enters academic details
User uploads resume
System extracts skills from resume text
Rule based engine processes all inputs
Career suggestions are generated and stored in Firebase
User can view insights and compare results with peers
# Project Goal

The goal of Mind2Career is to help students make informed career decisions using structured rules based on personality, academics, and skills instead of random selection.

# Future Improvements

• Add AI based recommendation system
• Improve resume parsing accuracy
• Add job market integration
• Enhance UI/UX design
• Add mentorship or guidance feature

# License

This project is developed for academic purposes.
