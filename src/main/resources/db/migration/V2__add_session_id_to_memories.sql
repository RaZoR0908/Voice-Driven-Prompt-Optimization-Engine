-- Add session_id column to memories table to track memory cards by session
ALTER TABLE memories ADD COLUMN session_id VARCHAR(100);

-- Create index on session_id for efficient filtering
CREATE INDEX idx_memories_session_id ON memories(session_id);
