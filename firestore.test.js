const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";
const ADMIN_UID = "admin_uid_001";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function adminContextDb() {
  return testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "nmd634384@gmail.com",
      email_verified: true,
    })
    .firestore();
}

function nowTs() {
  return new Date();
}

// --- 1. UNAUTHENTICATED ACCESS DENIALS ---

test("Unauthenticated user: cannot read tournaments, users, or reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("reports").get());
});

// --- 2. USERS PII ISOLATION & ROLE ESCALATION ---

test("Authenticated user: can create and read own profile, cannot read Bob's profile or escalate role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AlicePro",
      email: "alice@example.com",
      displayName: "Alice",
      role: "player",
      primaryGame: "FC Mobile",
      termsAccepted: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice can read own profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Bob cannot read Alice's profile (strict PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Alice cannot list all users
  await assertFails(aliceDb.collection("users").get());

  // Alice cannot escalate her own role to admin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: nowTs(),
    })
  );

  // Bob cannot create a profile with admin role directly
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "BobHacker",
      role: "admin",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

// --- 3. ALERTS & PLAYER STATS ---

test("Alerts: user can read and mark own alert read, cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("alerts").doc("alert_1").set({
      userId: ALICE_UID,
      title: "Match Ready",
      message: "Your Quarter-Final match is ready!",
      tournamentId: "tourn_1",
      matchId: "m_1",
      isRead: false,
      createdAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("alerts").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alerts").doc("alert_1").update({
      isRead: true,
    })
  );

  // Bob cannot read Alice's alerts
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("alerts").get());
});

test("PlayerStats: public query works with isPublic filter, cannot modify another user's stats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("playerStats").doc(ALICE_UID).set({
      userId: ALICE_UID,
      gamerTag: "AlicePro",
      primaryGame: "FC Mobile",
      matchesPlayed: 5,
      wins: 4,
      rating: 1350,
      isPublic: true,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("playerStats").where("isPublic", "==", true).get());
  await assertFails(aliceDb.collection("playerStats").get()); // missing isPublic filter
  await assertFails(
    bobDb.collection("playerStats").doc(ALICE_UID).update({
      wins: 999,
      updatedAt: nowTs(),
    })
  );
});

// --- 4. TOURNAMENTS, PARTICIPANTS & TEAMS ---

test("Tournaments: organizer creates free tournament, query aligns with isPublic, non-organizer cannot edit settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tourn_custom_1").set({
      title: "Dhaka Custom Free Cup",
      game: "FC Mobile",
      format: "Single Elimination",
      participationType: "Individual",
      maxParticipants: 16,
      participantCount: 0,
      registrationDeadlineText: "Tonight 8:00 PM BST",
      startTimeText: "Tonight 9:00 PM BST",
      matchDurationMinutes: 20,
      organizerId: ALICE_UID,
      organizerName: "AlicePro",
      inviteCode: "TAN-DHAKA-1",
      status: "Open",
      seedingMode: "Random",
      prizePoolText: "৳5,000 Sponsored Cup",
      prizePoolAmount: 5000,
      prizeSplitNote: "1st 60% • 2nd 25% • 3rd 15%",
      isPublic: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Negative prize pool amount rejected
  await assertFails(
    aliceDb.collection("tournaments").doc("tourn_bad_pool").set({
      title: "Invalid Pool Cup",
      game: "FC Mobile",
      format: "Single Elimination",
      participationType: "Individual",
      maxParticipants: 16,
      participantCount: 0,
      organizerId: ALICE_UID,
      inviteCode: "TAN-BAD-1",
      status: "Open",
      prizePoolAmount: -100,
      isPublic: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Aligned list query succeeds, unfiltered fails
  await assertSucceeds(bobDb.collection("tournaments").where("isPublic", "==", true).get());
  await assertFails(bobDb.collection("tournaments").get());

  // Non-organizer cannot edit tournament rules or title
  await assertFails(
    bobDb.collection("tournaments").doc("tourn_custom_1").update({
      title: "Hijacked Title",
      updatedAt: nowTs(),
    })
  );

  // Organizer can edit room details
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tourn_custom_1").update({
      roomId: "ROOM-889",
      roomPassword: "PASS1",
      updatedAt: nowTs(),
    })
  );
});

test("Participants & Teams: join open tournament, edit own registration with audit log, create & join team", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("tournaments").doc("tourn_team_1").set({
      title: "Free Fire Squad Open",
      game: "Free Fire",
      format: "Single Elimination",
      participationType: "Team",
      maxTeamSize: 4,
      maxParticipants: 16,
      participantCount: 0,
      organizerId: ALICE_UID,
      inviteCode: "FF-TEAM-01",
      status: "Open",
      isPublic: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Bob joins as participant
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tourn_team_1").collection("participants").doc(BOB_UID).set({
      tournamentId: "tourn_team_1",
      userId: BOB_UID,
      displayName: "BobSquad",
      gameFields: { uid: "1122334455", ign: "BobFF", level: "68" },
      publicStatLabel: "Lvl 68",
      sensitiveIdField: "1122334455",
      rankingMetric: 68,
      seedNumber: 0,
      auditLog: ["Joined at registration"],
      registeredAt: ts,
      updatedAt: ts,
    })
  );

  // Bob updates his participant entry before deadline
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tourn_team_1").collection("participants").doc(BOB_UID).update({
      publicStatLabel: "Lvl 70",
      rankingMetric: 70,
      auditLog: ["Joined at registration", "Edited level 68 -> 70"],
      updatedAt: nowTs(),
    })
  );

  // Bob creates a team and Charlie joins it
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tourn_team_1").collection("teams").doc("team_alpha").set({
      tournamentId: "tourn_team_1",
      name: "Dhaka Vipers",
      teamCode: "VIPER1",
      captainUserId: BOB_UID,
      memberIds: [BOB_UID],
      memberNames: ["BobSquad"],
      aggregatedStrength: 70,
      status: "Open",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    charlieDb.collection("tournaments").doc("tourn_team_1").collection("teams").doc("team_alpha").update({
      memberIds: [BOB_UID, CHARLIE_UID],
      memberNames: ["BobSquad", "CharlieFF"],
      aggregatedStrength: 135,
    })
  );
});

// --- 5. MATCHES, READY GATE, RESULT SUBMISSION & BRACKET ADVANCEMENT ---

test("Matches: organizer generates bracket, players ready -> submit -> confirm -> advance winner to nextMatchId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("tournaments").doc("tourn_b1").set({
      title: "FC Mobile Bracket Cup",
      game: "FC Mobile",
      format: "Single Elimination",
      participationType: "Individual",
      maxParticipants: 4,
      participantCount: 4,
      organizerId: ALICE_UID,
      inviteCode: "FCM-BRK",
      status: "In Progress",
      isPublic: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Charlie (non-organizer) cannot create matches
  await assertFails(
    charlieDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r1_1").set({
      tournamentId: "tourn_b1",
      organizerId: ALICE_UID,
      roundNumber: 1,
      roundName: "Semi-Finals",
      matchNumber: 1,
      status: "Scheduled",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Organizer creates Semi-Final 1 and Final
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r2_1").set({
      tournamentId: "tourn_b1",
      organizerId: ALICE_UID,
      roundNumber: 2,
      roundName: "Grand Final",
      matchNumber: 1,
      participant1Id: "",
      participant1UserId: "",
      participant1Name: "TBD",
      participant1Stat: "",
      participant1GameId: "",
      participant2Id: "",
      participant2UserId: "",
      participant2Name: "TBD",
      participant2Stat: "",
      participant2GameId: "",
      participant1Ready: false,
      participant2Ready: false,
      status: "Pending",
      winnerParticipantId: "",
      nextMatchId: "",
      nextMatchSlot: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r1_1").set({
      tournamentId: "tourn_b1",
      organizerId: ALICE_UID,
      roundNumber: 1,
      roundName: "Semi-Finals",
      matchNumber: 1,
      participant1Id: BOB_UID,
      participant1UserId: BOB_UID,
      participant1Name: "BobFC",
      participant1Stat: "OVR 104",
      participant1GameId: "99887766",
      participant2Id: CHARLIE_UID,
      participant2UserId: CHARLIE_UID,
      participant2Name: "CharlieFC",
      participant2Stat: "OVR 101",
      participant2GameId: "55443322",
      participant1Ready: false,
      participant2Ready: false,
      status: "Scheduled",
      winnerParticipantId: "",
      nextMatchId: "m_r2_1",
      nextMatchSlot: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob and Charlie mark Ready
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r1_1").update({
      participant1Ready: true,
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r1_1").update({
      participant2Ready: true,
      status: "Ready",
      updatedAt: nowTs(),
    })
  );

  // Bob submits result
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r1_1").update({
      score1: 3,
      score2: 1,
      submittedByUserId: BOB_UID,
      proofImageUri: "content://media/picker/0/1234",
      proofNote: "Full-time 3-1 screenshot attached",
      status: "Awaiting Results",
      auditLog: ["BobFC submitted 3-1"],
      updatedAt: nowTs(),
    })
  );

  // Charlie confirms result & advances Bob into Final slot 1
  await assertSucceeds(
    charlieDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r1_1").update({
      status: "Completed",
      winnerParticipantId: BOB_UID,
      winnerUserId: BOB_UID,
      winnerName: "BobFC",
      auditLog: ["BobFC submitted 3-1", "CharlieFC confirmed result"],
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    charlieDb.collection("tournaments").doc("tourn_b1").collection("matches").doc("m_r2_1").update({
      participant1Id: BOB_UID,
      participant1UserId: BOB_UID,
      participant1Name: "BobFC",
      participant1Stat: "OVR 104",
      participant1GameId: "99887766",
      status: "Scheduled",
      updatedAt: nowTs(),
    })
  );
});

// --- 6. TOURNAMENT CHAT & ADMIN DISPUTE REPORTS ---

test("Chat messages & Reports: player can chat and file dispute; only admin resolves reports", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const adminDb = adminContextDb();
  const ts = nowTs();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("tournaments").doc("tourn_chat").set({
      title: "Chat Arena",
      game: "eFootball",
      format: "Single Elimination",
      participationType: "Individual",
      maxParticipants: 8,
      participantCount: 2,
      organizerId: ALICE_UID,
      inviteCode: "EF-CHAT",
      status: "In Progress",
      isPublic: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Post chat message in Captains & Organizer channel + Lobby
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tourn_chat").collection("messages").doc("msg_1").set({
      tournamentId: "tourn_chat",
      senderId: BOB_UID,
      senderName: "BobFC",
      senderRole: "captain",
      teamName: "Dhaka Vipers",
      channel: "captains",
      isPinned: false,
      text: "Dhaka Vipers ready for Round 1 check-in!",
      isReported: false,
      createdAt: ts,
    })
  );

  // Non-organizer cannot pin messages
  await assertFails(
    charlieDb.collection("tournaments").doc("tourn_chat").collection("messages").doc("msg_1").update({
      isPinned: true,
    })
  );

  // Organizer (Alice) can pin important message
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tourn_chat").collection("messages").doc("msg_1").update({
      isPinned: true,
    })
  );

  // List aligned query works
  await assertSucceeds(
    charlieDb.collection("tournaments").doc("tourn_chat").collection("messages").where("tournamentId", "==", "tourn_chat").get()
  );

  // Charlie reports message
  await assertSucceeds(
    charlieDb.collection("tournaments").doc("tourn_chat").collection("messages").doc("msg_1").update({
      isReported: true,
      reportReason: "Spam",
    })
  );

  // Bob files dispute report
  await assertSucceeds(
    bobDb.collection("reports").doc("rep_1").set({
      type: "Match Dispute",
      tournamentId: "tourn_chat",
      matchId: "m_1",
      reporterId: BOB_UID,
      reporterName: "BobFC",
      reason: "Opponent disconnected at 88th minute",
      status: "Open",
      createdAt: ts,
    })
  );

  // Charlie cannot read Bob's report or list all reports
  await assertFails(charlieDb.collection("reports").doc("rep_1").get());
  await assertFails(charlieDb.collection("reports").get());

  // Admin can list and resolve report
  await assertSucceeds(adminDb.collection("reports").get());
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      status: "Resolved",
      resolutionNote: "Reviewed proof; awarded win to BobFC",
    })
  );
});
