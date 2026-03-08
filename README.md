# ⚽ Betting Intelligence Dashboard

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=javafx&logoColor=white)
![AI](https://img.shields.io/badge/Powered%20By-Gemini%20AI-4285F4?style=for-the-badge&logo=googlegemini&logoColor=white)

**Betting Intelligence Dashboard** is a high-performance desktop application designed for sophisticated sports data analysis and predictive modeling. The system integrates real-time market data from major European leagues and the **UEFA Champions League** with statistical algorithms and AI-driven insights.

---

## 📸 Dashboard Preview

### 🏟️ Main Dashboard & Smart Coupon Generation
![Main Dashboard](bet-gen/src/main/resources/images/main_dashboard.png)
*Modern Dark Mode UI with real-time odds analysis and automated coupon assembly.*

### 🔍 Deep Match Analysis & Form Tracking
![Detail View](bet-gen/src/main/resources/images/match_details.png)
*Deep-dive into team performance, historical win rates, and expected goal (xG) metrics.*

### 🤖 Gemini AI Expert (Interactive Chat)
![Chat Bot](bet-gen/src/main/resources/images/chat_bot.png)
*Advanced AI consultation with a streaming UI, providing real-time analysis of your betting slips.*

### 📈 BTTS Market Probability Ranking
![BTTS Analysis](bet-gen/src/main/resources/images/btts_analysis.png)
*Detailed probability modeling for "Both Teams To Score" markets across all supported leagues.*

---

## 🚀 Core Features

### 🧠 Smart Coupon Engine
* **Multi-League Support**: Analysis for EPL, La Liga, Serie A, Bundesliga, Ligue 1, and **UEFA Champions League**.
* **Statistical Modeling**: Analyzes matches based on Win Rate, BTTS probability, and Expected Goals (xG).
* **Risk Management**: Toggle between **SAFE**, **MEDIUM**, and **RISKY** strategy profiles.
* **Odds Targeting**: Dynamically assembles coupons to reach a user-defined total odd target.

### 🤖 AI Expert Integration (Google Gemini)
* **Interactive Consultation**: A built-in AI chatbot that reviews your generated coupons and provides professional betting advice.
* **Modern UI**: Features a professional, streaming response interface similar to the web version of Gemini.

### ⚡ Performance & Caching
* **Local Data Management**: Stores JSON responses in a secure directory (`~/.betaidashboard`) to minimize API latency and save token usage.
* **Offline Access**: Allows for detailed analysis of previously downloaded data without an internet connection.

---

## 🛠️ Technical Stack

* **Language**: Java 17 (Modular System).
* **Framework**: JavaFX 21 with custom CSS & **WebView** for AI rendering.
* **Data Processing**: Jackson Databind for high-speed JSON parsing.
* **Network**: Asynchronous `HttpClient` for real-time odds retrieval.

---

## 📦 Installation & Setup

1. **Get API Keys**:
    * Odds data: [The Odds API](https://the-odds-api.com/)
    * AI: [Google AI Studio](https://aistudio.google.com/)

2. **Configuration**:
   Create `src/main/resources/application.properties` (use `application.properties.example` as a template):
   
    ```properties
    bet.api.key=YOUR_ODDS_API_KEY
    football.api.key=YOUR_RAPIDAPI_KEY
    gemini.api.key=YOUR_GEMINI_API_KEY
    ```

### Build Process
   In IntelliJ IDEA, navigate to: Build -> Build Artifacts -> Rebuild.
### Deployment (macOS) 
   Run the automated update script from your terminal:
   ```Bash
      bash path_to_file/update_app.sh