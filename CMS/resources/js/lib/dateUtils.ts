/**
 * Format date to readable format
 */
export const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};

/**
 * Format date to short format (MM/DD/YYYY)
 */
export const formatDateShort = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US');
};
