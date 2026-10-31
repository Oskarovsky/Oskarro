package com.oskarro.oskarito.track;

import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Download credits. Each activity sets the balance to its own amount when that is more than what is left:
 * a like gives 3, a comment 5, a new track 7, and a new account starts with 3. Balances never add up, and each activity counts once per item.
 * Downloading a track for the first time spends one credit; downloading it again is free.
 */
@Service
public class DownloadAllowanceService {

    public enum Activity {
        LIKE(3), COMMENT(5), TRACK(7), WELCOME(3);

        public final int credits;

        Activity(int credits) {
            this.credits = credits;
        }
    }

    private final UserRepository users;
    private final DownloadGrantRepository grants;
    private final DownloadUnlockRepository unlocks;

    public DownloadAllowanceService(UserRepository users, DownloadGrantRepository grants, DownloadUnlockRepository unlocks) {
        this.users = users;
        this.grants = grants;
        this.unlocks = unlocks;
    }

    /** {@code item} tells repeats apart, e.g. "track:12" or "playlist:4". */
    @Transactional
    public void grant(User user, Activity activity, String item) {
        if (user == null || grants.existsByUserIdAndKindAndItemKey(user.getId(), activity.name(), item)) {
            return;
        }
        DownloadGrant grant = new DownloadGrant();
        grant.setUserId(user.getId());
        grant.setKind(activity.name());
        grant.setItemKey(item);
        grant.setCreatedAt(Instant.now());
        try {
            grants.saveAndFlush(grant);
        } catch (DataIntegrityViolationException ex) {
            return;
        }
        if (credits(user) < activity.credits) {
            user.setDownloadCredits(activity.credits);
            users.save(user);
        }
    }

    public int credits(User user) {
        return user == null || user.getDownloadCredits() == null ? 0 : user.getDownloadCredits();
    }

    public Set<Integer> unlockedAmong(User user, Collection<Integer> trackIds) {
        if (user == null || trackIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(unlocks.unlockedAmong(user.getId(), trackIds));
    }

    /** True when the user may download now; spends a credit the first time a track is downloaded. */
    @Transactional
    public boolean redeem(User user, Integer trackId) {
        if (unlocks.existsByUserIdAndTrackId(user.getId(), trackId)) {
            return true;
        }
        if (credits(user) <= 0) {
            return false;
        }
        DownloadUnlock unlock = new DownloadUnlock();
        unlock.setUserId(user.getId());
        unlock.setTrackId(trackId);
        unlock.setCreatedAt(Instant.now());
        try {
            unlocks.saveAndFlush(unlock);
        } catch (DataIntegrityViolationException ex) {
            // A double click raced the first one, which already paid.
            return true;
        }
        user.setDownloadCredits(credits(user) - 1);
        users.save(user);
        return true;
    }
}
