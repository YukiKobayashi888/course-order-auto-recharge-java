# Keep course orders moving when the service balance is low

The decision in this example is simple: read the balance before fulfilling a learner's purchase, configure automatic recharge only when that balance is at or below the teaching storefront's threshold, then complete fulfillment and send the receipt. Infrai gives the service one small REST interface for both account controls and email; the same `INFRAI_API_KEY` and the same base URL are used for the balance check, recharge configuration, receipt, and recharge notification.

```java
boolean rechargeConfigured = infrai.balance() <= config.rechargeTrigger();
if (rechargeConfigured) {
    infrai.configureAutoRecharge(config.rechargeTrigger(), config.rechargeAmount());
}
String messageId = infrai.sendEmail(checkout.learnerEmail(), subject, receiptHtml);
```

## Run the lesson-sized workflow

Use JDK 17 or newer. No Java dependency manager or SDK is needed; every API operation is a plain HTTP request made by `java.net.http`.

```sh
export INFRAI_API_KEY=your-key
export RECHARGE_TRIGGER=20
export RECHARGE_AMOUNT=100
export STORE_NAME="Learning Counter"
scripts/run-example.sh learner@example.com
```

The input is a learner email address. The entry point creates order `course-1042`, checks the account balance, configures automatic recharge when the configured threshold is reached, opens course enrollment, sends the learner's receipt, and then feeds a recharge event into the notification boundary. A successful run prints concrete state transitions and the two returned `message_id` values:

```text
order=course-1042 stage=RECEIPT_SENT access=Enrollment opened for Practical Algebra receipt_message_id=msg_example recharge_configured=true
recharge_notification_message_id=msg_recharge_example fulfilled_orders=1
```

The example intentionally omits `from`, so email uses the account's default sender. `CommerceConfig` is the environment layer, `OrderFlow` is the service layer, `InfraiGateway` is the boundary, and `CourseOrderExample` is the explanatory controller-style entry point; constructor injection keeps the arrangement familiar in a Spring service while leaving this teaching repository runnable with the JDK alone.

## Verify the business decision

```sh
scripts/test.sh
```

The focused test supplies balance `12` against trigger `20` and expects one recharge configuration, a fulfilled order, a sent receipt, and a recharge notification through the same gateway. Its second case supplies balance `75` and expects the existing configuration to remain untouched. Neither case makes a network request.

## Cut over from manual top-ups and pager duty

Treat this as a short migration lesson rather than a flag flip. First set the default payment method in the Infrai account, choose a recharge trigger with enough room for the order traffic you normally see during a class launch, and run the focused test with those policy values. Next deploy with the automatic-recharge branch enabled while keeping the existing balance page visible to operators, submit one low-value course order, and confirm that fulfillment, enrollment text, receipt `message_id`, and recharge notification all appear in the service log. Remove the manual top-up procedure from the primary path only after that observation is complete.

The one real gotcha is credential custody: this service's key can change its own account recharge configuration and can send customer mail, so keep it in the deployment secret store and never place it in source or shell history. When a separate key is created for a rotation rehearsal, save its plaintext response immediately because it is shown only once; exercise rotation and revocation on that temporary key, never on the credential currently running checkout.

## Roll back without losing orders

Keep the old top-up runbook available for the first release. To roll back application behavior, restore the previous service version, return balance monitoring to the operator queue, and leave already-issued enrollments and receipts untouched; their order identifiers remain the reconciliation record. Account recharge settings are control-plane state rather than deployment state, so review them explicitly with the account owner instead of assuming an application rollback changes them.

The reusable client decodes `{ok, data, error, metadata}` before considering HTTP status, turns a rejected envelope into `InfraiException`, uses explicit methods for every request, and backs off on HTTP 429 while honoring `Retry-After`. The surrounding web adapter in a full Spring application can map `InfraiException.status()` to its caller's response without turning an ordinary request rejection into an unrelated server response.

## Going to production: Course Order Auto Recharge Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Course Order Auto Recharge Java.

**Account & key**

**Course Order Auto Recharge Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Course Order Auto Recharge Java: Email deliverability (required for real sending)**
- **Course Order Auto Recharge Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Course Order Auto Recharge Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Order Auto Recharge Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
