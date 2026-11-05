-- Add color column to project_areas for visual identification in the UI
ALTER TABLE project_areas
    ADD COLUMN IF NOT EXISTS color VARCHAR(20);
