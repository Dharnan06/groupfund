# GroupFund - Final Project Notes

GroupFund is a group savings and loan management system for small self-help groups.

## Final activation workflow

1. A user creates a Savings or Loan group.
2. During group creation, the leader selects the planned member limit: minimum 5, maximum 20.
3. The group starts as `PENDING_MEMBERS`.
4. When at least 5 members have accepted, the group becomes `READY_TO_ACTIVATE`.
5. The leader clicks `Request Group Activation`.
6. The group becomes `ACTIVATION_PENDING`.
7. All currently accepted members must approve activation.
8. After all accepted members approve, the group becomes `ACTIVE`.
9. Payments are locked until the group is active.
10. Only the leader can record savings/EMI payments from `My Group -> Open Group -> Payments`.

## Important security note

Mail username/password, JWT secret, and Cloudinary API secret now use environment variables instead of hard-coded secrets.

Set these if OTP email and image upload are required:

- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `JWT_SECRET`
- `CLOUDINARY_API_SECRET`

Database defaults still work locally with:

- DB: `nammakuzhu`
- username: `root`
- password: `root123`

You can override using:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
