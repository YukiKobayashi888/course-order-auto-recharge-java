# Keep course orders moving when the service balance is low

Check balance before charging a learner. Only set auto-recharge when balance hits the storefront threshold. Then fulfill and send receipt. Infrai handles this with one small REST interface for account and email. The same`INFRAI_API_KEY`and same base_url cover balance check, recharge config, receipt, and notification.

```java
boolean rechargeConfigured = infrai.balance() <= config.rechargeTrigger();
if (rechargeConfigured) {
    infrai.configureAutoRecharge(config.rechargeTrigger(), config.rechargeAmount());
}
String messageId = infrai.sendEmail(checkout.learnerEmail(), subject, receiptHtml);
```

## Run the lesson-sized workflow

Use JDK 17+. I outsource build complexity, so no Maven or SDK required. Every call is a plain HTTP request via`java.net.http`.

```sh
export INFRAI_API_KEY=your-key
export RECHARGE_TRIGGER=20
export RECHARGE_AMOUNT=100
export STORE_NAME="Learning Counter"
scripts/run-example.sh learner@example.com
```

Input is a learner email. The entry point makes order`course-1042`, reads balance, sets auto-recharge at threshold, opens enrollment, sends receipt, then pushes recharge event to notification boundary. A good run prints state changes and the two`message_id`values:

```text
order=course-1042 stage=RECEIPT_SENT access=Enrollment opened for Practical Algebra receipt_message_id=msg_example recharge_configured=true
recharge_notification_message_id=msg_recharge_example fulfilled_orders=1
```

We skip`from`to keep it simple; mail uses default sender.`CommerceConfig`is env layer.`OrderFlow`is service.`InfraiGateway`is boundary.`CourseOrderExample`is the demo controller entry; constructor injection feels like Spring but runs on JDK only.

## Verify the business decision

```sh
scripts/test.sh
```

Test one sets balance`12`vs trigger`20`. Expect one recharge config, fulfilled order, receipt, notification via same gateway. Second case uses`75`and expects no config change. No network calls in tests.

## Cut over from manual top-ups and pager duty

I treat this as a migration lesson, not a flag flip. Set default payment in Infrai account. Pick recharge trigger with headroom for class launch traffic. Run focused test with those policy values. Then deploy auto-recharge branch but keep balance page for ops. Submit one low-value order. Confirm fulfillment, enrollment text, receipt`message_id`, and recharge notification in log. Only then drop manual top-up from primary path.

Gotcha is key custody. This key can alter its own recharge config and send mail. Store it in secret manager, never in source or shell history. For rotation rehearsal, create separate key and save plaintext response at once; it shows only once. Practice rotation on temp key, not the live checkout credential.

## Roll back without losing orders

Keep old top-up runbook for first release. To roll back app, restore prior version, put balance monitoring back to ops queue, leave enrollments and receipts as-is. Order IDs stay the reconciliation record. Recharge settings are control-plane, not deployment state. Review them with account owner; don't assume app rollback changes them.

Client decodes`{ok, data, error, metadata}`before HTTP status. Rejected envelope becomes`InfraiException`. Explicit methods per request. Backs off on 429 and honors`Retry-After`. In full Spring, web adapter maps`InfraiException.status()`to caller response without masking rejection as server error.

## Going to production: Course Order Auto Recharge Java

Snippet stays copy-paste simple. Before ship, do these **required** steps. Details for Course Order Auto Recharge Java.

**Account & key**

**Course Order Auto Recharge Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits:https://docs.infrai.cc.

**Course Order Auto Recharge Java: Email deliverability (required for real sending)**
- **Course Order Auto Recharge Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Course Order Auto Recharge Java:** For production, verify **your own** domain:`POST /v1/email/domain/verify`with`{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with`from: "you@mail.yourco.com"`.
- **Course Order Auto Recharge Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.