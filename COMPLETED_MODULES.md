# GroupFund Completed Modules

## Added / completed

1. Root app opening

   * Added `src/main/resources/static/index.html`.
   * Opening `http://localhost:8080` redirects to `/app/auth/login.html` after running the backend.

2. Finance details in group details

   * Savings group shows monthly savings, start date, accepted members, and monthly group collection.
   * Custom loan group shows loan source, loan amount, monthly EMI, duration, interest rate, and start date.

3. Payment tracking module

   * Backend APIs:

      * `GET /api/payments/group/{groupId}`
      * `GET /api/payments/group/{groupId}/summary`
      * `POST /api/payments/record`
   * Leader can record member payments.
   * Members can view payment status and history.
   * Summary shows expected, paid, pending, and status for each accepted member.

4. Dashboard stats

   * Dashboard uses backend API for total groups, monthly savings, active loan amount, and pending dues.

5. Profile fixes

   * Full name edit updates both profile and main user name.
   * Aadhaar number is returned in profile response so frontend edit form can show it.
   * Profile image upload is integrated with Cloudinary.

6. Group rule improvements

   * Group member limit supports minimum 5 and maximum 20 members.
   * Group starts as `PENDING_MEMBERS`.
   * After at least 5 accepted members, the group becomes `READY_TO_ACTIVATE`.
   * Leader can request activation.
   * Accepted members approve activation.
   * Group becomes `ACTIVE` only after all accepted members approve.
   * Payments are locked until the group is active.

7. Stability fixes

   * JWT filter handles invalid, expired, or corrupted tokens safely.
   * Mail template loading works after packaging as a JAR.
   * Browser favicon issue fixed.
   * Static frontend pages are served through Spring Boot.

## How to run

1. Start MySQL and keep database name as `nammakuzhu`.
2. Open the project in IntelliJ.
3. Run `NammakuzhuApplication`.
4. Open:

   `http://localhost:8080`

   or directly:

   `http://localhost:8080/app/auth/login.html`

## Important security note

Mail username/password, JWT secret, and Cloudinary API secret use environment variables instead of hard-coded secrets.

Set these environment variables before running OTP email and image upload:

* `MAIL_USERNAME`
* `MAIL_PASSWORD`
* `JWT_SECRET`
* `CLOUDINARY_API_SECRET`

Database defaults work locally with:

* DB: `nammakuzhu`
* username: `root`
* password: `root123`

Database values can be overridden using:

* `DB_URL`
* `DB_USERNAME`
* `DB_PASSWORD`

Before uploading this project publicly, do not commit real passwords, API secrets, app passwords, or private keys.
