package com.example.mind2career

// ─────────────────────────────────────────────────────────────────────────────
// RecommendationEngine.kt  —  Rule-based career recommendation engine
// Inputs : personality, grade, field, percentage, skills (all from Firebase)
// Output : CareerRoadmap
// ─────────────────────────────────────────────────────────────────────────────

data class CareerRoadmap(
    val recommendedCareers : List<String>,
    val primaryCareer      : String,
    val whyThisCareer      : String,
    val roadmapSteps       : List<RoadmapStep>,
    val skillsToLearn      : List<String>,
    val skillsYouHave      : List<String>,
    val strongSubjects     : List<String>,
    val weakSubjects       : List<String>,
    val personalityInsight : String,
    val estimatedTimeline  : String,
    val performanceLabel   : String,
    val performanceColor   : String
)

data class RoadmapStep(
    val stepNumber  : Int,
    val title       : String,
    val description : String,
    val duration    : String
)

object RecommendationEngine {

    // ── 32 personality types → career families ────────────────────────────────
    private val personalityCareerMap: Map<String, List<String>> = mapOf(
        "Executor"      to listOf("Software Engineering", "Product Management", "DevOps Engineering", "Tech Lead", "Startup Founder"),
        "Charmer"       to listOf("Product Management", "UX/UI Design", "IT Consulting", "Developer Relations", "Team Lead"),
        "Builder"       to listOf("Full Stack Development", "Software Architecture", "Engineering Management", "CTO Track"),
        "Balanced"      to listOf("Backend Development", "Database Administration", "QA Engineering", "Systems Analysis"),
        "Visionary"     to listOf("AI/ML Engineering", "Research & Development", "Product Strategy", "Data Science", "Innovation Lead"),
        "Worrier"       to listOf("Cybersecurity", "QA/Testing Engineer", "Compliance & Risk", "Network Administration", "DevSecOps"),
        "Nurturer"      to listOf("UX Research", "Technical Training", "EdTech Development", "Accessibility Engineering"),
        "Caretaker"     to listOf("IT Support & Operations", "Project Management", "Business Analysis", "ERP Consulting"),
        "Luminary"      to listOf("CTO", "Chief Architect", "AI Research Lead", "Tech Entrepreneur", "Engineering Director"),
        "Empath"        to listOf("UX/UI Design", "Product Design", "HR Tech", "Community Management", "EdTech"),
        "Creator"       to listOf("Game Development", "Creative Coding", "Frontend Engineering", "Interactive Media", "AR/VR Development"),
        "Sensitive"     to listOf("Data Analysis", "Backend Development", "Research Engineer", "Technical Writing", "QA Engineering"),
        "Companion"     to listOf("Full Stack Development", "Developer Advocate", "Open Source Contributor", "Tech Community Lead"),
        "Striver"       to listOf("Software Engineering", "Cloud Engineering", "Solutions Architect", "Competitive Programming"),
        "Explorer"      to listOf("Machine Learning", "Data Science", "Research Engineering", "Blockchain Development", "Quantum Computing"),
        "Socializer"    to listOf("Developer Relations", "Tech Sales", "IT Consulting", "Product Management", "Community Manager"),
        "Advocate"      to listOf("Product Management", "Engineering Management", "IT Policy", "Tech Ethics", "Consulting"),
        "Idealist"      to listOf("AI Ethics Researcher", "EdTech Engineer", "Social Impact Tech", "NGO Tech Lead"),
        "Humanist"      to listOf("UX Design", "EdTech", "Health Informatics", "Accessibility Tech", "Social Computing"),
        "Expressionist" to listOf("Frontend Development", "UI Design", "Game Design", "Creative Technologist", "Digital Art"),
        "Dynamo"        to listOf("Startup CTO", "Lead Engineer", "Product Engineering", "Innovation Consultant", "Platform Architect"),
        "Achiever"      to listOf("Software Engineer", "Cloud Solutions", "DevOps", "Backend Engineering", "Database Engineering"),
        "Connector"     to listOf("Developer Advocate", "Tech Community Lead", "Product Manager", "Open Source Lead"),
        "Perfectionist" to listOf("Cybersecurity Engineer", "Software Quality Lead", "Systems Engineer", "Code Auditor", "DevSecOps"),
        "Adventurer"    to listOf("Game Developer", "AR/VR Engineer", "Blockchain Developer", "Robotics Engineer", "Embedded Systems"),
        "Leader"        to listOf("Engineering Manager", "Tech Lead", "Project Manager", "Solutions Architect", "CTO Track"),
        "Harmonizer"    to listOf("Scrum Master", "Project Manager", "Team Lead", "IT Consultant", "Business Analyst"),
        "Performer"     to listOf("Frontend Developer", "UI Engineer", "Creative Technologist", "Tech Presenter", "Developer Advocate"),
        "Mystic"        to listOf("AI/ML Research", "Data Science", "NLP Engineering", "Computer Vision", "Deep Learning"),
        "Seeker"        to listOf("Research Engineer", "Data Engineer", "Blockchain", "Cloud Architecture", "Tech Exploration"),
        "Dreamer"       to listOf("UX Designer", "Creative Developer", "EdTech", "AI Art", "Human-Computer Interaction"),
        "Innovator"     to listOf("AI Engineering", "Product Innovation", "R&D Engineer", "Startup Technical Founder", "ML Engineer")
    )

    // ── Personality insights ──────────────────────────────────────────────────
    private val personalityInsights: Map<String, String> = mapOf(
        "Executor"      to "You lead with confidence, execute with discipline, and think with imagination. You turn bold visions into solid plans.",
        "Charmer"       to "You combine social charisma with emotional warmth and real reliability. People trust and love working with you.",
        "Builder"       to "You bring together leadership, creativity, and discipline. You build things that are both high-performing and human.",
        "Balanced"      to "You are emotionally stable, adaptable, and consistently trustworthy. You thrive in structured, reliable environments.",
        "Visionary"     to "You are empathetic and creatively inspired. You see possibilities others miss and build futures others only imagine.",
        "Worrier"       to "You are deeply reliable and self-aware. Your attention to detail and care for quality makes you exceptional in precision roles.",
        "Nurturer"      to "You combine creativity, kindness, and discipline. You make others feel both inspired and genuinely cared for.",
        "Caretaker"     to "You reliably deliver while genuinely caring for people. You build deep trust through consistency and warmth.",
        "Luminary"      to "You are the most multidimensional personality — you inspire, create, deliver, care, and feel with extraordinary depth.",
        "Empath"        to "You are extraordinarily emotionally attuned. Your deep loyalty and self-awareness make you invaluable in people-centered roles.",
        "Creator"       to "You produce creative work of exceptional depth. You combine disciplined execution with emotional honesty and imaginative vision.",
        "Sensitive"     to "You are deeply self-aware and highly empathetic. Your thoughtfulness and reflection produce careful, high-quality work.",
        "Companion"     to "You bring social magnetism, emotional warmth, and creative vision together. You build rare connections wherever you go.",
        "Striver"       to "You are highly motivated across social, professional, and emotional dimensions. You hold yourself accountable to excellence.",
        "Explorer"      to "You are exceptionally creative and a fast learner. You absorb new information quickly and apply it in original ways.",
        "Socializer"    to "You are a natural leader who energizes people. Your communication skills and network-building ability open every door.",
        "Advocate"      to "You lead with authority and empathy. You are deeply responsible and make decisions with both head and heart.",
        "Idealist"      to "You combine creativity, care, discipline, and emotional depth. You are driven to build things that genuinely matter.",
        "Humanist"      to "You create rich environments where people feel welcome and intellectually alive. You combine warmth with curiosity.",
        "Expressionist" to "You build connections of extraordinary emotional depth. Your authentic presence and expressiveness inspire everyone around you.",
        "Dynamo"        to "You bring social energy, creative vision, emotional depth, and disciplined execution together in one rare package.",
        "Achiever"      to "You are extremely reliable and always keep your promises. You achieve goals through focus, discipline, and real effort.",
        "Connector"     to "You naturally build strong friendships and communities. You energize groups while creating genuine warmth and connection.",
        "Perfectionist" to "You are extraordinarily responsible and feel genuine concern for doing things right. You combine precision with emotional intelligence.",
        "Adventurer"    to "You inspire others with enthusiasm for new ideas. You adapt quickly, create freely, and pursue experiences boldly.",
        "Leader"        to "You excel at leading teams through motivation and strong planning. You combine communication with real accountability.",
        "Harmonizer"    to "You are deeply empathetic and excellent at resolving conflicts. You create environments where people feel safe and heard.",
        "Performer"     to "You are deeply passionate and bring real emotional energy to everything. People always know exactly where you stand.",
        "Mystic"        to "You have exceptional emotional depth combined with creative and intellectual richness. You see meaning where others see noise.",
        "Seeker"        to "You bring social energy, emotional depth, and creative vision together. You inspire others through authentic expression.",
        "Dreamer"       to "You combine emotional depth, creative brilliance, and genuine warmth. You bring rare insight and imagination to every challenge.",
        "Innovator"     to "You turn creative ideas into real, completed, high-quality outcomes. You combine imagination with the discipline to finish."
    )

    // ── Career → 5 roadmap steps ──────────────────────────────────────────────
    private fun getRoadmapForCareer(career: String, percentage: Int): List<RoadmapStep> {
        val isWeak = percentage < 50
        return when {
            career.contains("Software Engin", true) ||
                    career.contains("Full Stack", true) ||
                    career.contains("Backend", true) -> listOf(
                RoadmapStep(1, "Strengthen DSA Fundamentals",
                    if (isWeak) "Start with arrays, linked lists, and sorting on LeetCode Easy. Do 2 problems per day."
                    else "Tackle trees, graphs, and dynamic programming. Aim for 50+ LeetCode problems.",
                    if (isWeak) "2–3 months" else "1 month"),
                RoadmapStep(2, "Pick a Backend Language",
                    "Master Kotlin/Java for Android backend, or Python/Node.js for web. Build 2 small projects.", "2 months"),
                RoadmapStep(3, "Learn Databases & REST APIs",
                    "Firebase (you already know!), SQL basics, and REST API design. Build a CRUD app.", "1–2 months"),
                RoadmapStep(4, "Build a Full Portfolio Project",
                    "Create a complete project: user auth, database, REST API, and frontend. Deploy it online.", "2 months"),
                RoadmapStep(5, "Interview Prep & Apply",
                    "Practice coding interviews, system design, and behavioral questions. Apply to internships.", "1–2 months")
            )
            career.contains("AI", true) || career.contains("ML", true) ||
                    career.contains("Machine Learning", true) || career.contains("Data Science", true) -> listOf(
                RoadmapStep(1, "Math & Stats Foundation",
                    if (isWeak) "Start with Khan Academy: linear algebra, probability, statistics. Take your time."
                    else "Review linear algebra, calculus, and probability at university level.",
                    if (isWeak) "2–3 months" else "1 month"),
                RoadmapStep(2, "Python for Data Science",
                    "Learn Python, NumPy, Pandas, Matplotlib. Complete CS50P or fast.ai course.", "1–2 months"),
                RoadmapStep(3, "Machine Learning Basics",
                    "Study supervised/unsupervised learning with scikit-learn. Complete Andrew Ng's ML course.", "2 months"),
                RoadmapStep(4, "Deep Learning Projects",
                    "Learn TensorFlow or PyTorch. Build 2 projects: image classifier + text analyzer.", "2–3 months"),
                RoadmapStep(5, "Specialize & Publish",
                    "Pick NLP, Computer Vision, or RecSys. Upload projects to GitHub and Kaggle.", "2 months")
            )
            career.contains("Cybersecurity", true) || career.contains("DevSecOps", true) -> listOf(
                RoadmapStep(1, "Networking Fundamentals",
                    "Study OSI model, TCP/IP, DNS, HTTP. Complete CompTIA Network+ prep materials.", "1–2 months"),
                RoadmapStep(2, "Linux & Command Line",
                    "Get comfortable with Linux terminal. Practice on TryHackMe beginner rooms.", "1 month"),
                RoadmapStep(3, "Security Concepts",
                    "Study encryption, authentication, and OWASP Top 10. Take Security+ prep.", "2 months"),
                RoadmapStep(4, "Ethical Hacking Practice",
                    "Use Kali Linux. Practice on HackTheBox or TryHackMe. Complete 10 rooms.", "2 months"),
                RoadmapStep(5, "Certification & Portfolio",
                    "Aim for CompTIA Security+ or CEH. Write a blog about your CTF experiences.", "2–3 months")
            )
            career.contains("UX", true) || career.contains("Design", true) -> listOf(
                RoadmapStep(1, "Design Fundamentals",
                    "Study color theory, typography, and UI principles. Read 'Don't Make Me Think'.", "1 month"),
                RoadmapStep(2, "Master Figma",
                    "Complete Figma tutorials. Redesign 3 existing apps as practice projects.", "1–2 months"),
                RoadmapStep(3, "UX Research Methods",
                    "Learn user interviews, usability testing, personas, and user journey maps.", "1 month"),
                RoadmapStep(4, "Build Portfolio Case Studies",
                    "Design 2 original apps end-to-end: research → wireframe → prototype → test.", "2–3 months"),
                RoadmapStep(5, "Apply & Get Feedback",
                    "Share on Behance/Dribbble, apply for internships, join design communities.", "Ongoing")
            )
            career.contains("Cloud", true) || career.contains("DevOps", true) -> listOf(
                RoadmapStep(1, "Linux & Bash Scripting",
                    "Master Linux commands and Bash scripting. Automate basic daily tasks.", "1 month"),
                RoadmapStep(2, "Cloud Platform Basics",
                    "Start with AWS or Google Cloud free tier. Complete the Cloud Practitioner course.", "1–2 months"),
                RoadmapStep(3, "Containers & CI/CD",
                    "Learn Docker, then Kubernetes basics. Set up a GitHub Actions pipeline.", "2 months"),
                RoadmapStep(4, "Infrastructure as Code",
                    "Learn Terraform or Ansible. Deploy a full cloud infrastructure project.", "1–2 months"),
                RoadmapStep(5, "Get Certified",
                    "Aim for AWS Solutions Architect Associate or Google Associate Cloud Engineer.", "2 months")
            )
            career.contains("Game", true) -> listOf(
                RoadmapStep(1, "Game Dev Fundamentals",
                    "Learn Unity (C#) or Unreal (C++). Complete the official beginner tutorials.", "1–2 months"),
                RoadmapStep(2, "Build Mini Games",
                    "Build 3 small games: a platformer, a puzzle, and a shooter. Focus on finishing.", "2 months"),
                RoadmapStep(3, "Game Physics & UI",
                    "Study 2D/3D physics, animations, and in-game UI design.", "1 month"),
                RoadmapStep(4, "Publish on Play Store",
                    "Polish one game and publish it. Even a small audience gives you real credibility.", "2 months"),
                RoadmapStep(5, "Portfolio & Community",
                    "Share on itch.io, join game jams (Ludum Dare), and apply to game studios.", "Ongoing")
            )
            career.contains("Product Man", true) -> listOf(
                RoadmapStep(1, "PM Fundamentals",
                    "Read 'Inspired' by Marty Cagan. Learn product thinking, roadmaps, and prioritization.", "1 month"),
                RoadmapStep(2, "Data & Analytics",
                    "Learn SQL basics and Google Analytics. PMs must understand data to make decisions.", "1 month"),
                RoadmapStep(3, "User Research & Prototyping",
                    "Learn Figma for wireframes. Conduct 5 user interviews for a problem you care about.", "1–2 months"),
                RoadmapStep(4, "Work on Real Products",
                    "Contribute to open source, join a startup as volunteer PM, or build your own side project.", "2–3 months"),
                RoadmapStep(5, "Apply & Network",
                    "Apply for APM programs (Google, Meta), build your LinkedIn, and network actively.", "Ongoing")
            )
            else -> listOf(
                RoadmapStep(1, "Build Core Technical Skills",
                    "Strengthen your fundamentals through online courses and daily practice.", "1–2 months"),
                RoadmapStep(2, "Learn Industry Tools",
                    "Identify the top 3 tools in your target career and master them through projects.", "2 months"),
                RoadmapStep(3, "Build Real Projects",
                    "Create 2–3 projects that demonstrate your skills. Quality over quantity always.", "2 months"),
                RoadmapStep(4, "Build Your Online Portfolio",
                    "Put projects on GitHub. Write a LinkedIn profile. Create a personal website.", "1 month"),
                RoadmapStep(5, "Apply & Keep Learning",
                    "Apply for internships or junior roles. Treat rejections as feedback. Keep building.", "Ongoing")
            )
        }
    }

    // ── Skill gap ─────────────────────────────────────────────────────────────
    private fun getSkillsToLearn(career: String, existingSkills: List<String>): List<String> {
        val map: Map<String, List<String>> = mapOf(
            "Software Engineering"   to listOf("DSA", "System Design", "Git", "REST APIs", "SQL", "Unit Testing"),
            "Full Stack Development" to listOf("React", "Node.js", "SQL", "Docker", "Git", "REST APIs"),
            "AI/ML Engineering"      to listOf("Python", "TensorFlow", "Linear Algebra", "Pandas", "scikit-learn", "Statistics"),
            "Data Science"           to listOf("Python", "SQL", "Pandas", "Matplotlib", "Machine Learning", "Statistics"),
            "Cybersecurity"          to listOf("Linux", "Networking", "Ethical Hacking", "Encryption", "OWASP", "Kali Linux"),
            "Cloud Engineering"      to listOf("AWS", "Docker", "Kubernetes", "Terraform", "Linux", "CI/CD"),
            "DevOps Engineering"     to listOf("Docker", "Kubernetes", "CI/CD", "Linux", "Bash Scripting", "Monitoring"),
            "UX/UI Design"           to listOf("Figma", "User Research", "Prototyping", "Wireframing", "Usability Testing"),
            "Product Management"     to listOf("SQL", "Figma", "Data Analysis", "Roadmap Planning", "User Research"),
            "Game Development"       to listOf("Unity", "C#", "Game Physics", "3D Modeling Basics", "Animation"),
            "Backend Development"    to listOf("Java", "SQL", "REST APIs", "Docker", "System Design"),
            "Machine Learning"       to listOf("Python", "TensorFlow", "PyTorch", "Statistics", "Data Preprocessing"),
            "Frontend Development"   to listOf("HTML/CSS", "JavaScript", "React", "Figma", "Responsive Design")
        )
        val key = map.keys.firstOrNull {
            career.contains(it, ignoreCase = true) || it.contains(career, ignoreCase = true)
        } ?: return listOf("Git", "Problem Solving", "Communication")

        val required = map[key] ?: emptyList()
        val have     = existingSkills.map { it.lowercase().trim() }
        return required.filter { skill ->
            have.none { h -> h.contains(skill.lowercase()) || skill.lowercase().contains(h) }
        }.take(5)
    }

    // ── Why this career ───────────────────────────────────────────────────────
    private fun buildWhy(
        personality: String, career: String,
        field: String, percentage: Int, skills: List<String>
    ): String {
        val perf = when {
            percentage >= 80 -> "Your strong academic performance in $field ($percentage%) shows real technical aptitude."
            percentage >= 60 -> "Your solid results in $field give you a good foundation to build upon."
            percentage >= 40 -> "While your $field score ($percentage%) has room to grow, your personality strengths compensate."
            else             -> "Your $field results ($percentage%) mean extra effort on core concepts — but your $personality personality suits this path."
        }
        val skillNote = if (skills.isNotEmpty())
            "Your skills in ${skills.take(3).joinToString(", ")} directly support this direction."
        else "Building practical skills will be your most important immediate step."
        return "$perf Your $personality personality naturally fits $career. $skillNote"
    }

    // ── Performance label + color ─────────────────────────────────────────────
    private fun perfLabel(pct: Int): Pair<String, String> = when {
        pct >= 80 -> "Excellent" to "#4CAF50"
        pct >= 60 -> "Good"      to "#2196F3"
        pct >= 40 -> "Average"   to "#FF9800"
        else      -> "Needs Work" to "#F44336"
    }

    // ── Timeline ──────────────────────────────────────────────────────────────
    private fun timeline(pct: Int, skills: List<String>): String {
        val base = when {
            pct >= 75 -> "4–6 months"
            pct >= 50 -> "6–9 months"
            else      -> "9–12 months"
        }
        return if (skills.size >= 4) "$base (your skills speed this up!)"
        else "$base to first opportunity"
    }

    // ── Career ranking ────────────────────────────────────────────────────────
    private fun rankCareers(
        careers: List<String>, field: String,
        percentage: Int, skills: List<String>
    ): List<String> {
        val bonusMap: Map<String, List<String>> = mapOf(
            "Data Structures & Algorithms" to listOf("Software", "AI", "ML", "Backend", "Full Stack", "Competitive"),
            "Computer Networks"            to listOf("Cloud", "Cybersecurity", "DevOps", "Network", "Backend"),
            "Operating Systems"            to listOf("DevOps", "Cloud", "Systems", "Embedded", "Backend"),
            "Software Engineering"         to listOf("Product", "Engineering", "Full Stack", "Software"),
            "ITC"                          to listOf("IT Support", "Business Analyst", "Consulting"),
            "physics"                      to listOf("Engineering", "Robotics", "Embedded", "Systems"),
            "maths"                        to listOf("Data Science", "AI", "ML", "Algorithm", "Software"),
            "computer"                     to listOf("Software", "Web", "Mobile", "Game"),
            "biology"                      to listOf("Health", "Bioinformatics", "EdTech"),
            "english"                      to listOf("Technical Writing", "Developer Relations", "Product Manager")
        )
        val fieldNorm = field.lowercase()
        val bonus = bonusMap.entries
            .firstOrNull { (k, _) -> fieldNorm.contains(k.lowercase()) || k.lowercase().contains(fieldNorm) }
            ?.value ?: emptyList()
        val normSkills = skills.map { it.lowercase() }

        return careers.sortedByDescending { c ->
            var score = 0
            if (bonus.any { c.contains(it, true) }) score += 3
            if (percentage >= 70 && c.contains("Engineer", true)) score += 2
            if (percentage < 50  && c.contains("Design",   true)) score += 1
            normSkills.forEach { s -> if (c.lowercase().contains(s)) score += 1 }
            score
        }
    }

    // ── MAIN ENTRY POINT ─────────────────────────────────────────────────────
    fun generateRoadmap(
        personality : String,
        grade       : String,
        field       : String,
        percentage  : Int,
        skills      : List<String>
    ): CareerRoadmap {

        val careers       = personalityCareerMap[personality] ?: personalityCareerMap["Balanced"]!!
        val ranked        = rankCareers(careers, field, percentage, skills)
        val primary       = ranked.first()
        val (label, color)= perfLabel(percentage)

        return CareerRoadmap(
            recommendedCareers = ranked.take(3),
            primaryCareer      = primary,
            whyThisCareer      = buildWhy(personality, primary, field, percentage, skills),
            roadmapSteps       = getRoadmapForCareer(primary, percentage),
            skillsToLearn      = getSkillsToLearn(primary, skills),
            skillsYouHave      = skills,
            strongSubjects     = if (percentage >= 70) listOf(field) else emptyList(),
            weakSubjects       = if (percentage < 70)  listOf(field) else emptyList(),
            personalityInsight = personalityInsights[personality]
                ?: "You have a unique combination of strengths well-suited for the tech industry.",
            estimatedTimeline  = timeline(percentage, skills),
            performanceLabel   = label,
            performanceColor   = color
        )
    }
}

