# CAPTCHA App

## About

CAPTCHA app generated using github copilot and connects to OpenAI API to generate CAPTCHAs.

---

## How the Application Was Generated

1. **Spring Boot starter application** created manually using IDE.

2. **GitHub Copilot** set in Agent mode with Claude Haiku 4.5.
   - Prompted Copilot to create the required UI and backend in the Spring Boot app:

     > "Hi, I want you to generate a simple user interface which generates a CAPTCHA. The UI should contain just this CAPTCHA and one button with label **Decipher**. Clicking this button should decipher the CAPTCHA and populate the CAPTCHA into a text box. The text box should be below the CAPTCHA. Also, the CAPTCHA should be only alphanumeric - randomly generated. Add a **Refresh** icon near this CAPTCHA which, when clicked, should generate a new CAPTCHA and clear the text box. This webpage should be available in this Spring Boot application — perhaps under the `/captcha` page."

   - Later on prompted copilot few times to make corrections/edits to the generated code as necessary in the same mode. No code added manually

3. **Application pushed to GitHub** manually.

---

## Application Features

- UI displays a CAPTCHA screen with a **Refresh** button and a **Decipher** button to read from the CAPTCHA and display in a text box.
- Alphanumeric CAPTCHA is generated using **OpenAI** (connected via OpenAI API key).
- Log shows the payload containing the instructions each time.

**Sample log:**

```
LLM request payload: {
  "temperature": 1.0,
  "max_tokens": 10,
  "messages": [
    {
      "role": "user",
      "content": "Generate a single random 6-character alphanumeric string (letters and digits). Ensure there is at least one letter and one digit. Return only the string with no extra text."
    }
  ],
  "model": "gpt-3.5-turbo"
}
```

---

## Demo

*Demo video/screenshot in file https://github.com/tkt009/capcha-app/blob/release1/CapchaAppDemo_CopilotGenerated.mp4.zip*
