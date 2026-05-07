package com.example.mind2career

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs
import kotlin.math.roundToInt

class RankingScreen : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rankingscreen)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        loadingOverlay = findViewById(R.id.loadingOverlay)

        setupBottomNav()
        loadMyData()
    }

    // ===================== BOTTOM NAV =====================

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_ranking
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, home_activity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_ranking -> true
                R.id.nav_transition -> {
                    startActivity(Intent(this, transitionScreen::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // ===================== LOADING =====================

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ===================== LOAD MY DATA =====================

    private fun loadMyData() {
        showLoading(true)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            showLoading(false)
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    showLoading(false)
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val personality    = doc.getString("personality") ?: "N/A"
                val rawScore       = doc.getLong("score")?.toInt() ?: 0
                val total          = doc.getLong("total")?.toInt() ?: 1
                val personalityPct = if (total > 0)
                    ((rawScore.toDouble() / total.toDouble()) * 100).roundToInt() else 0
                val mindStrength   = doc.getLong("percentage")?.toInt() ?: 0
                val overallScore   = ((personalityPct + mindStrength) / 2.0).roundToInt()

                @Suppress("UNCHECKED_CAST")
                val mySkills = (doc.get("skills") as? List<String>) ?: emptyList()

                updateMyUI(overallScore, personality, mindStrength)
                loadRankingAndPeers(uid, overallScore, mySkills)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ===================== LOAD RANKING & PEERS =====================

    private fun loadRankingAndPeers(
        myUid: String,
        myOverallScore: Int,
        mySkills: List<String>
    ) {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->

                data class PeerData(
                    val uid: String,
                    val personality: String,
                    val overallScore: Int,
                    val skills: List<String>
                )

                val allUsers = mutableListOf<PeerData>()

                for (doc in snapshot.documents) {
                    val raw  = doc.getLong("score")?.toInt() ?: continue
                    val tot  = doc.getLong("total")?.toInt() ?: 1
                    val pPct = if (tot > 0)
                        ((raw.toDouble() / tot) * 100).roundToInt() else 0
                    val mind    = doc.getLong("percentage")?.toInt() ?: 0
                    val overall = ((pPct + mind) / 2.0).roundToInt()

                    @Suppress("UNCHECKED_CAST")
                    val skills = (doc.get("skills") as? List<String>) ?: emptyList()

                    allUsers.add(
                        PeerData(
                            uid         = doc.id,
                            personality = doc.getString("personality") ?: "",
                            overallScore = overall,
                            skills      = skills
                        )
                    )
                }

                allUsers.sortByDescending { it.overallScore }

                val myRank     = allUsers.indexOfFirst { it.uid == myUid } + 1
                val closePeers = allUsers
                    .filter { it.uid != myUid && abs(it.overallScore - myOverallScore) <= 10 }
                    .take(5)
                val interestMatch = calcInterestMatch(mySkills, closePeers.map { it.skills })

                runOnUiThread {
                    findViewById<TextView>(R.id.tvRankText).text =
                        if (myRank > 0) "🏆  You are ranked #$myRank" else "Rank not available"

                    findViewById<TextView>(R.id.tvInterestMatch).text = "$interestMatch%"
                    findViewById<ProgressBar>(R.id.pbInterestMatch).progress = interestMatch

                    buildPeersList(
                        closePeers.map { Triple(it.uid, it.personality, it.overallScore) },
                        myUid
                    )
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ===================== INTEREST MATCH =====================

    private fun calcInterestMatch(
        mine: List<String>,
        peersSkills: List<List<String>>
    ): Int {
        if (mine.isEmpty() || peersSkills.isEmpty()) return 0
        val mySet   = mine.map { it.lowercase() }.toSet()
        val peerSet = peersSkills.flatten().map { it.lowercase() }.toSet()
        if (peerSet.isEmpty()) return 0
        val common = mySet.intersect(peerSet).size
        val union  = mySet.union(peerSet).size
        return if (union == 0) 0 else ((common.toDouble() / union) * 100).roundToInt()
    }

    // ===================== UPDATE UI =====================

    private fun updateMyUI(
        overallScore: Int,
        personality: String,
        mindStrengthPct: Int
    ) {
        findViewById<TextView>(R.id.tvUserScore).text        = overallScore.toString()
        findViewById<TextView>(R.id.tvPersonalityType).text  = personality
        findViewById<TextView>(R.id.tvMindStrength).text     = "$mindStrengthPct%"
        findViewById<ProgressBar>(R.id.pbMindStrength).progress = mindStrengthPct
    }

    // ===================== BUILD PEERS LIST =====================

    private fun buildPeersList(
        peers: List<Triple<String, String, Int>>,
        myUid: String
    ) {
        val container   = findViewById<LinearLayout>(R.id.llPeersList)
        val noPeersText = findViewById<TextView>(R.id.tvNoPeers)

        // Pehle 2 children (header + noPeers text) chhod ke baaki remove karo
        while (container.childCount > 2) {
            container.removeViewAt(2)
        }

        if (peers.isEmpty()) {
            noPeersText.text       = "No close peers found yet"
            noPeersText.visibility = View.VISIBLE
            return
        }
        noPeersText.visibility = View.GONE

        val dp = resources.displayMetrics.density

        for ((index, peer) in peers.withIndex()) {
            val peerUid     = peer.first
            val personality = peer.second
            val score       = peer.third
            val isMe        = peerUid == myUid

            // Row
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity     = android.view.Gravity.CENTER_VERTICAL
            row.setPadding(0, (14 * dp).toInt(), 0, (14 * dp).toInt())
            if (isMe) row.setBackgroundColor(0x1A7C3AED.toInt())

            // Rank circle
            val rankSize = (28 * dp).toInt()
            val rankLp   = LinearLayout.LayoutParams(rankSize, rankSize)
            rankLp.marginEnd = (12 * dp).toInt()
            val rankView = TextView(this)
            rankView.layoutParams = rankLp
            rankView.text     = "${index + 1}"
            rankView.textSize = 12f
            rankView.gravity  = android.view.Gravity.CENTER
            rankView.setTextColor(if (isMe) 0xFFFFFFFF.toInt() else 0xFF7C3AED.toInt())
            rankView.background = ContextCompat.getDrawable(
                this,
                if (isMe) R.drawable.bg_rank_circle_filled
                else R.drawable.bg_rank_circle_outline
            )

            // Avatar
            val avatarSize = (38 * dp).toInt()
            val avatarLp   = LinearLayout.LayoutParams(avatarSize, avatarSize)
            avatarLp.marginEnd = (12 * dp).toInt()
            val avatar = FrameLayout(this)
            avatar.layoutParams = avatarLp
            avatar.background   = ContextCompat.getDrawable(
                this,
                if (isMe) R.drawable.bg_avatar_purple else R.drawable.bg_avatar_light
            )
            val iconSize = (18 * dp).toInt()
            val iconLp   = FrameLayout.LayoutParams(iconSize, iconSize)
            iconLp.gravity = android.view.Gravity.CENTER
            val iconView = ImageView(this)
            iconView.layoutParams = iconLp
            iconView.setImageResource(R.drawable.ic_default_user)
            iconView.imageTintList = android.content.res.ColorStateList.valueOf(
                if (isMe) 0xFFFFFFFF.toInt() else 0xFF7C3AED.toInt()
            )
            avatar.addView(iconView)

            // Name + tag column
            val nameColLp = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            val nameCol = LinearLayout(this)
            nameCol.orientation  = LinearLayout.VERTICAL
            nameCol.layoutParams = nameColLp

            val nameView = TextView(this)
            nameView.text     = "User ${index + 1}"
            nameView.textSize = 14f
            nameView.setTextColor(if (isMe) 0xFF7C3AED.toInt() else 0xFF111827.toInt())
            if (isMe) nameView.setTypeface(
                nameView.typeface, android.graphics.Typeface.BOLD
            )

            val tagView = TextView(this)
            tagView.text     = personality
            tagView.textSize = 11f
            tagView.setTextColor(0xFF9CA3AF.toInt())

            nameCol.addView(nameView)
            nameCol.addView(tagView)

            // Score
            val scoreView = TextView(this)
            scoreView.text     = "$score%"
            scoreView.textSize = 14f
            scoreView.setTextColor(if (isMe) 0xFF7C3AED.toInt() else 0xFF6B7280.toInt())
            if (isMe) scoreView.setTypeface(
                scoreView.typeface, android.graphics.Typeface.BOLD
            )

            row.addView(rankView)
            row.addView(avatar)
            row.addView(nameCol)
            row.addView(scoreView)
            container.addView(row)

            // Divider
            if (index < peers.size - 1) {
                val divider = View(this)
                divider.setBackgroundColor(0xFFF3F4F6.toInt())
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                container.addView(divider)
            }
        }
    }
}