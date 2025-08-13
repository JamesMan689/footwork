# Streak System with Automatic Database Synchronization

## Overview
The Footwork application now has an intelligent streak system that automatically keeps the database in sync with calculated streak values, ensuring consistency between stored and real-time data.

## How It Works

### Smart Streak Calculation
- **Real-time Calculation**: Streaks are calculated based on current date vs last completion date
- **Automatic Sync**: Database is automatically updated when discrepancies are detected
- **History Preservation**: All changes are logged for audit purposes

### Streak Logic
- **0 days missed**: Streak continues (same day completion)
- **1 day missed**: Streak continues (user can still complete today)
- **2+ days missed**: Streak automatically resets to 0

## Key Methods

### `getCurrentStreak(UserInfo user)`
- **Purpose**: Get current streak with automatic database synchronization
- **Behavior**: 
  - Calculates what the streak should be
  - Updates database if there's a discrepancy
  - Returns the calculated value
- **Use Case**: API endpoints, user profile updates

### `getCurrentStreakReadOnly(UserInfo user)`
- **Purpose**: Get current streak without modifying database
- **Behavior**: 
  - Calculates streak value
  - Does NOT update database
  - Returns calculated value
- **Use Case**: Read-only operations, bulk calculations

### `syncAllUserStreaks()`
- **Purpose**: Manually sync all user streaks in database
- **Behavior**: 
  - Iterates through all users
  - Updates database values to match calculated values
  - Returns count of updated users
- **Use Case**: Admin operations, system maintenance

## API Endpoints

### Get User Streak
- **Endpoint**: `GET /api/user/streak`
- **Behavior**: Returns current streak, automatically syncs database if needed
- **Response**: Real-time streak value

### User Profile
- **Endpoint**: `GET /api/user/profile`
- **Behavior**: Includes current streak calculation, may trigger database sync
- **Response**: Profile with accurate streak information

### Admin Streak Sync
- **Endpoint**: `POST /api/admin/sync-streaks`
- **Access**: Admin users only
- **Behavior**: Manually syncs all user streaks in database
- **Response**: Count of updated users

## Database Synchronization

### Automatic Sync
- **Trigger**: Called whenever `getCurrentStreak()` is invoked
- **Condition**: When stored value ≠ calculated value
- **Action**: Updates database to reflect current reality
- **Logging**: Records all changes for audit purposes

### Manual Sync
- **Trigger**: Admin endpoint or scheduled task
- **Scope**: All users in the system
- **Use Case**: Bulk updates, system maintenance, data consistency

### Sync Logic
```java
// Calculate what the streak should be
if (daysSinceLastCompletion > 1) {
    calculatedStreak = 0;  // Streak broken
} else if (daysSinceLastCompletion == 1) {
    calculatedStreak = user.getStreak();  // Streak can continue
} else {
    calculatedStreak = user.getStreak();  // Streak continues
}

// Update database if there's a discrepancy
if (calculatedStreak != user.getStreak()) {
    user.setStreak(calculatedStreak);
    repository.save(user);
}
```

## Example Scenarios

### Scenario 1: Consecutive Days
- **Aug 10**: User completes plan, streak = 1
- **Aug 11**: User completes plan, streak = 2
- **Aug 12**: User completes plan, streak = 3
- **Database**: Always matches calculated value

### Scenario 2: Missed Day
- **Aug 10**: User completes plan, streak = 1
- **Aug 11**: User skips (missed 1 day)
- **Aug 12**: User completes plan, streak = 2 (continues)
- **Database**: Updated to reflect current state

### Scenario 3: Broken Streak
- **Aug 10**: User completes plan, streak = 1
- **Aug 11**: User skips (missed 1 day)
- **Aug 12**: User skips (missed 2 days)
- **Aug 13**: Database automatically updated: streak = 0
- **API Response**: streak = 0 (matches database)

## Benefits of Auto-Sync

### Consistency
- ✅ **Database Accuracy**: Stored values always match calculated values
- ✅ **Single Source of Truth**: No more discrepancies between stored and calculated
- ✅ **Real-time Updates**: Database reflects current state immediately

### Performance
- ✅ **Reduced Calculations**: No need to recalculate on every API call
- ✅ **Efficient Queries**: Database queries return accurate values directly
- ✅ **Cached Results**: Can cache database values with confidence

### User Experience
- ✅ **Accurate Information**: Users see consistent streak information
- ✅ **Immediate Updates**: Changes are reflected instantly
- ✅ **Reliable Data**: No confusion about current streak status

## Logging and Audit

### Change Tracking
- **Discrepancy Detection**: Logs when stored ≠ calculated values
- **Update Recording**: Records all database changes
- **User Identification**: Tracks which user's streak was updated
- **Reason Logging**: Documents why the change occurred

### Log Examples
```
INFO: Streak discrepancy detected for user user@example.com: stored=1, calculated=0, days since completion=2
INFO: Database updated: streak changed from 1 to 0
INFO: Streak sync completed: 3 users updated
```

## Testing the System

### 1. Check Current Streak
```bash
curl -X GET http://localhost:8080/api/user/streak \
  -H "Authorization: Bearer <access_token>"
```

### 2. Verify Database Sync
- Check application logs for sync messages
- Verify database values match API responses
- Use admin endpoint to force sync if needed

### 3. Admin Sync
```bash
curl -X POST http://localhost:8080/api/admin/sync-streaks \
  -H "Authorization: Bearer <admin_token>"
```

## Best Practices

### For Developers
1. **Use `getCurrentStreak()`** for API endpoints that need current values
2. **Use `getCurrentStreakReadOnly()`** for bulk operations or read-only scenarios
3. **Monitor logs** for automatic sync operations
4. **Use admin sync** for maintenance and data consistency

### For Administrators
1. **Monitor sync logs** to track automatic updates
2. **Use manual sync** after system updates or data migrations
3. **Verify consistency** between database and API responses
4. **Check user feedback** about streak accuracy

## Troubleshooting

### Common Issues
1. **Streak Still Wrong**: Check if sync method is being called
2. **Database Not Updated**: Verify user has proper permissions
3. **Logs Missing**: Check logging configuration
4. **Performance Issues**: Monitor sync frequency and impact

### Debug Steps
1. **Check API Response**: Verify calculated streak value
2. **Check Database**: Compare stored vs calculated values
3. **Check Logs**: Look for sync and update messages
4. **Manual Sync**: Use admin endpoint to force update

## Future Enhancements

### Advanced Features
- **Scheduled Sync**: Automatic daily sync of all user streaks
- **Batch Updates**: Efficient bulk database operations
- **Change History**: Track all streak changes over time
- **Notification System**: Alert users when streaks are broken

### Performance Optimizations
- **Caching**: Cache calculated values to reduce computation
- **Async Updates**: Background sync operations
- **Incremental Sync**: Only update changed values
- **Database Indexing**: Optimize streak-related queries
