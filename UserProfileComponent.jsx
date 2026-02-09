import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { logger } from './utils/logger';

/**
 * UserProfile Component
 * Displays user information with bio, posts, and activity feed
 */
const UserProfile = ({ userId }) => {
  const [user, setUser] = useState(null);
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch user data on component mount or userId change
  useEffect(() => {
    const fetchUserData = async () => {
      setLoading(true);
      setError(null);

      try {
        // Parallel API calls for better performance
        const [userResponse, postsResponse] = await Promise.all([
          fetch(`/api/users/${userId}`),
          fetch(`/api/users/${userId}/posts`)
        ]);

        if (!userResponse.ok) {
          throw new Error(`Failed to fetch user: ${userResponse.status}`);
        }

        if (!postsResponse.ok) {
          throw new Error(`Failed to fetch posts: ${postsResponse.status}`);
        }

        const userData = await userResponse.json();
        const postsData = await postsResponse.json();

        setUser(userData);
        setPosts(postsData);
      } catch (err) {
        logger.error('Error fetching user data', { userId, error: err.message });
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [userId]); // Proper dependency array

  // Memoized calculation for user stats
  const userStats = useMemo(() => {
    if (!posts || posts.length === 0) {
      return { totalPosts: 0, avgLikes: 0, totalComments: 0 };
    }

    const totalPosts = posts.length;
    const totalLikes = posts.reduce((sum, post) => sum + (post.likes || 0), 0);
    const totalComments = posts.reduce((sum, post) => sum + (post.comments || 0), 0);

    return {
      totalPosts,
      avgLikes: Math.round(totalLikes / totalPosts),
      totalComments
    };
  }, [posts]);

  // Event handler with proper cleanup
  useEffect(() => {
    const handleResize = () => {
      logger.debug('Window resized', { width: window.innerWidth });
    };

    window.addEventListener('resize', handleResize);

    // Cleanup function to prevent memory leaks
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  // Callback for handling post deletion
  const handleDeletePost = useCallback(async (postId) => {
    try {
      const response = await fetch(`/api/posts/${postId}`, {
        method: 'DELETE'
      });

      if (!response.ok) {
        throw new Error('Failed to delete post');
      }

      // Update state immutably
      setPosts(prevPosts => prevPosts.filter(post => post.id !== postId));
      logger.info('Post deleted successfully', { postId });
    } catch (err) {
      logger.error('Error deleting post', { postId, error: err.message });
    }
  }, []);

  if (loading) {
    return <div className="loading">Loading user profile...</div>;
  }

  if (error) {
    return <div className="error" role="alert">Error: {error}</div>;
  }

  if (!user) {
    return <div className="not-found">User not found</div>;
  }

  return (
    <div className="user-profile" data-testid="user-profile">
      <header className="profile-header">
        <img
          src={user.avatar || '/default-avatar.png'}
          alt={`${user.name}'s avatar`}
          loading="lazy"
          className="avatar"
        />
        <h1>{user.name}</h1>
        <p className="username">@{user.username}</p>
      </header>

      {/* XSS VULNERABILITY HERE - Using dangerouslySetInnerHTML with user input */}
      <section className="bio-section">
        <h2>About</h2>
        <div
          className="user-bio"
          dangerouslySetInnerHTML={{ __html: user.bio }}
        />
      </section>

      <section className="stats-section">
        <h2>Statistics</h2>
        <div className="stats-grid">
          <div className="stat-card">
            <span className="stat-value">{userStats.totalPosts}</span>
            <span className="stat-label">Posts</span>
          </div>
          <div className="stat-card">
            <span className="stat-value">{userStats.avgLikes}</span>
            <span className="stat-label">Avg Likes</span>
          </div>
          <div className="stat-card">
            <span className="stat-value">{userStats.totalComments}</span>
            <span className="stat-label">Comments</span>
          </div>
        </div>
      </section>

      <section className="posts-section">
        <h2>Recent Posts</h2>
        {posts.length === 0 ? (
          <p className="no-posts">No posts yet</p>
        ) : (
          <ul className="posts-list">
            {posts.map(post => (
              <li key={post.id} className="post-item">
                <h3>{post.title}</h3>
                <p>{post.content}</p>
                <button
                  onClick={() => handleDeletePost(post.id)}
                  aria-label={`Delete post ${post.title}`}
                  className="delete-button"
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
};

export default UserProfile;
