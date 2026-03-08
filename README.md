# ⚽ Betting Intelligence Dashboard

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=javafx&logoColor=white)
![AI](https://img.shields.io/badge/Powered%20By-Gemini%20AI-4285F4?style=for-the-badge&logo=googlegemini&logoColor=white)

**Betting Intelligence Dashboard** is a high-performance desktop application designed for sophisticated sports data analysis and predictive modeling. The system integrates real-time market data with statistical algorithms and AI-driven insights to help users identify high-value betting opportunities.

---

## 📸 Dashboard Preview

|                            Generated Coupon                             | Match Detail Analysis |
|:-----------------------------------------------------------------------:|:---:|
| ![Main Dashboard](bet-gen/src/main/resources/images/main_dashboard.png) | ![Detail View](bet-gen/src/main/resources/images/match_details.png) |
|               *Modern Dark Mode UI with real-time stats*                | *Deep-dive into team form and xG* |


|              AI Chat Expert               |             BTTS Analysis View             |
|:-----------------------------------------:|:------------------------------------------:|
|     ![Chat Bot](bet-gen/src/main/resources/images/chat_bot.png)      |    ![BTTS Analysis](bet-gen/src/main/resources/images/btts_analysis.png)     |
| *Interactive AI advice via Google Gemini* | *Detailed probability ranking for markets* |
---

## 🚀 Core Features

### 🧠 Smart Coupon Engine
* **Statistical Modeling**: Analyzes hundreds of matches based on Win Rate, BTTS (Both Teams To Score) probability, and Expected Goals (xG).
* **Risk Management**: Toggle between **SAFE**, **MEDIUM**, and **RISKY** strategy profiles.
* **Odds Targeting**: Dynamically assembles coupons to reach a user-defined total odd target.

### 📊 Comprehensive BTTS Analysis
* **Market Probability**: Calculates the likelihood of both teams scoring based on historical performance.
* **Match Status**: Categorizes games into HIGH, MEDIUM, or LOW probability zones for quick decision-making.

### 🤖 AI Expert Integration (Google Gemini)
* **Interactive Consultation**: A built-in AI chatbot that reviews your generated coupons and provides professional betting advice.
* **Data Interpretation**: Converts complex statistical data into natural language insights.

### ⚡ Performance & Caching
* **Local Data Management**: Stores JSON responses in a secure directory (`~/.betaidashboard`) to minimize API latency and save token usage.
* **Offline Access**: Allows for detailed analysis of previously downloaded data without an internet connection.

---

## 🛠️ Technical Stack

* **Language**: Java 17 (Modular System).
* **Framework**: JavaFX 21 with custom CSS (Modern Dark Mode).
* **Data Processing**: Jackson Databind for high-speed JSON parsing.
* **Network**: Asynchronous `HttpClient` for real-time odds retrieval.

---

## 📦 Installation & Setup

To run this application, you need to obtain API keys from the following providers:

1. **Betting Odds**: Register at [The Odds API](https://the-odds-api.com/).
2. **Historical Stats**: Obtain an API key for **API-Football** via [RapidAPI](https://rapidapi.com/api-sports/api/api-football/).
3. **AI Insights**: Obtain a Gemini AI key from [Google AI Studio](https://aistudio.google.com/).

### Configuration
Create a file named `application.properties` in `src/main/resources/` and fill in your keys:
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