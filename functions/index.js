// NEW v2 Imports
const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

// NEW V2 GLOBAL OPTIONS
// Add these two lines
const {setGlobalOptions} = require("firebase-functions/v2");
setGlobalOptions({region: "asia-southeast1"});

// Initialize Admin SDK
initializeApp();

// Send notification when a chore is assigned
exports.sendChoreAssignedNotification = onDocumentCreated("households/{householdId}/chores/{choreId}", async (event) => {
  const snap = event.data;
  if (!snap) return null; // No data

  const chore = snap.data();
  const assignedToId = chore.assignedTo;

  // Get the assigned user's FCM token
  const userDoc = await getFirestore()
      .collection("users")
      .doc(assignedToId)
      .get();

  if (!userDoc.exists) return null;

  const fcmToken = userDoc.data().fcmToken;
  if (!fcmToken) return null;

  // Get the creator's name
  const creatorDoc = await getFirestore()
      .collection("users")
      .doc(chore.createdBy)
      .get();

  const creatorName = creatorDoc.exists ? creatorDoc.data().displayName : "Someone";

  const message = {
    token: fcmToken,
    notification: {
      title: "New Chore Assigned",
      body: `${creatorName} assigned you: ${chore.title}`,
    },
    data: {
      type: "chore_assigned",
      chore_id: event.params.choreId, // Use event.params
      chore_title: chore.title,
      due_date: new Date(chore.dueDate).toLocaleDateString(),
    },
  };

  return getMessaging().send(message);
});

// Send notification when a chore is completed
exports.sendChoreCompletedNotification = onDocumentUpdated("households/{householdId}/chores/{choreId}", async (event) => {
  if (!event.data) return null; // No data change

  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  // Check if chore was just completed
  if (!beforeData.isCompleted && afterData.isCompleted) {
    const householdId = event.params.householdId; // Use event.params

    // Get household members
    const householdDoc = await getFirestore()
        .collection("households")
        .doc(householdId)
        .get();

    if (!householdDoc.exists) return null;

    const memberIds = householdDoc.data().memberIds;

    // Get the completer's name
    const completerDoc = await getFirestore()
        .collection("users")
        .doc(afterData.assignedTo)
        .get();

    const completerName = completerDoc.exists ? completerDoc.data().displayName : "Someone";

    // Send notification to all members except the one who completed it
    const promises = memberIds
        .filter((memberId) => memberId !== afterData.assignedTo)
        .map(async (memberId) => {
          const memberDoc = await getFirestore()
              .collection("users")
              .doc(memberId)
              .get();

          if (!memberDoc.exists) return null;

          const fcmToken = memberDoc.data().fcmToken;
          if (!fcmToken) return null;

          const message = {
            token: fcmToken,
            notification: {
              title: "Chore Completed!",
              body: `${completerName} completed: ${afterData.title}`,
            },
            data: {
              type: "chore_completed",
              chore_id: event.params.choreId, // Use event.params
              chore_title: afterData.title,
            },
          };

          return getMessaging().send(message);
        });

    return Promise.all(promises);
  }

  return null;
});

// Send notification when a new member joins the household
exports.sendNewMemberNotification = onDocumentUpdated("households/{householdId}", async (event) => {
  if (!event.data) return null; // No data change

  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  const beforeMembers = beforeData.memberIds || [];
  const afterMembers = afterData.memberIds || [];

  // Check if a new member was added
  const newMembers = afterMembers.filter((id) => !beforeMembers.includes(id));

  if (newMembers.length === 0) return null;

  // Get the new member's name
  const newMemberDoc = await getFirestore()
      .collection("users")
      .doc(newMembers[0])
      .get();

  const newMemberName = newMemberDoc.exists ? newMemberDoc.data().displayName : "Someone";

  // Send notification to all existing members
  const promises = beforeMembers.map(async (memberId) => {
    const memberDoc = await getFirestore()
        .collection("users")
        .doc(memberId)
        .get();

    if (!memberDoc.exists) return null;

    const fcmToken = memberDoc.data().fcmToken;
    if (!fcmToken) return null;

    const message = {
      token: fcmToken,
      notification: {
        title: "New Household Member",
        body: `${newMemberName} joined ${afterData.name}`,
      },
      data: {
        type: "new_member",
        household_id: event.params.householdId, // Use event.params
      },
    };

    return getMessaging().send(message);
  });

  return Promise.all(promises);
});

// Send notification when a new note is created
exports.sendNewNoteNotification = onDocumentCreated("households/{householdId}/notes/{noteId}", async (event) => {
  const snap = event.data;
  if (!snap) return null; // No data

  const note = snap.data();
  const householdId = event.params.householdId; // Use event.params

  // Get household members
  const householdDoc = await getFirestore()
      .collection("households")
      .doc(householdId)
      .get();

  if (!householdDoc.exists) return null;

  const memberIds = householdDoc.data().memberIds;

  // Get the creator's name
  const creatorDoc = await getFirestore()
      .collection("users")
      .doc(note.createdBy)
      .get();

  const creatorName = creatorDoc.exists ? creatorDoc.data().displayName : "Someone";

  // Send notification to all members except the creator
  const promises = memberIds
      .filter((memberId) => memberId !== note.createdBy)
      .map(async (memberId) => {
        const memberDoc = await getFirestore()
            .collection("users")
            .doc(memberId)
            .get();

        if (!memberDoc.exists) return null;

        const fcmToken = memberDoc.data().fcmToken;
        if (!fcmToken) return null;

        const message = {
          token: fcmToken,
          notification: {
            title: "New Note",
            body: `${creatorName} posted: ${note.title}`,
          },
          data: {
            type: "new_note",
            note_id: event.params.noteId,
          },
        };

        return getMessaging().send(message);
      });

  return Promise.all(promises);
});
