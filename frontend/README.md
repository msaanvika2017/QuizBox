# QuizBox frontend

React + Vite frontend for the QuizBox Spring Boot demo.

## Run locally

Start the backend from the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

Start the frontend from this folder:

```powershell
npm.cmd run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to `http://localhost:8080`.

## Demo roles

- **Teacher:** Open demo dashboard for creating and publishing quizzes and viewing student attempt summaries.
- **Student:** Enter a name and student ID to browse published quizzes, submit answers, and review the score.

Role selection and student details are not authentication. Do not use real student data or sensitive exams.

## Checks

```powershell
npm.cmd run build
npm.cmd run lint
```
