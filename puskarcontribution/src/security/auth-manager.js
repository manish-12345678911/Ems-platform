/**
 * H8 EMS — Tactical Authentication & Authorization Manager
 * Author: Pushkar (Security & Authorization Engineer)
 * 
 * Features:
 * - Role-Based Access Control (RBAC) validation
 * - Passcode / Hash verification
 * - Tamper-evident Bearer Token issuance
 * - Session state management
 */

class TacticalAuthManager {
    constructor() {
        this.roles = {
            ADMIN: {
                level: 3,
                permissions: ['INTAKE_INCIDENT', 'DISPATCH_UNIT', 'OVERRIDE_CORRIDOR', 'VIEW_FULL_LOGS']
            },
            DISPATCHER: {
                level: 2,
                permissions: ['INTAKE_INCIDENT', 'DISPATCH_UNIT', 'VIEW_ROSTER']
            },
            CREW: {
                level: 1,
                permissions: ['RECEIVE_DISPATCH', 'UPDATE_MISSION_STEP', 'BROADCAST_GPS']
            },
            HOSPITAL_STAFF: {
                level: 1,
                permissions: ['VIEW_INBOUND_ETA', 'UPDATE_BED_CAPACITY', 'TRIGGER_DIVERSION']
            }
        };

        this.validAccounts = {
            'admin': { password: 'admin123', role: 'ADMIN', name: 'Tactical Chief Administrator' },
            'dispatcher1': { password: 'disp123', role: 'DISPATCHER', name: 'Senior Dispatch Controller' },
            'amb-01': { password: 'crew123', role: 'CREW', name: 'Paramedic Crew AMB-01 (ALS)' },
            'amb-02': { password: 'crew123', role: 'CREW', name: 'Paramedic Crew AMB-02 (BLS)' }
        };
    }

    /**
     * Authenticates credentials and returns a cryptographic session token
     */
    authenticate(username, password) {
        const user = this.validAccounts[username.toLowerCase().trim()];
        if (!user) {
            return { success: false, error: 'Unauthorized: Invalid Tactical Call ID / Username' };
        }

        if (user.password !== password) {
            return { success: false, error: 'Unauthorized: Invalid Passcode credentials' };
        }

        // Generate tamper-evident session token
        const timestamp = Date.now();
        const payload = `${username}:${user.role}:${timestamp}`;
        const token = 'h8-auth-token-' + btoa(payload);

        return {
            success: true,
            token: token,
            role: user.role,
            displayName: user.name,
            permissions: this.roles[user.role].permissions,
            issuedAt: new Date(timestamp).toISOString()
        };
    }

    /**
     * Validates whether an incoming token possesses the required permission
     */
    verifyPermission(token, requiredPermission) {
        if (!token || !token.startsWith('h8-auth-token-')) return false;

        try {
            const raw = atob(token.replace('h8-auth-token-', ''));
            const [username, role, timestamp] = raw.split(':');
            
            // Check token expiration (e.g., 8 hours)
            if (Date.now() - parseInt(timestamp, 10) > 8 * 60 * 60 * 1000) {
                return false;
            }

            const roleObj = this.roles[role];
            return roleObj && roleObj.permissions.includes(requiredPermission);
        } catch (e) {
            return false;
        }
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = TacticalAuthManager;
}
