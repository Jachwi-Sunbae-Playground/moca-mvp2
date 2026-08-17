import type { LinkProps } from 'react-router-dom';
import { ButtonLink } from './Button';
import Icon from './Icon';
import styles from './AddItemLink.module.css';

type AddItemLinkProps = Omit<LinkProps, 'children'> & {
  children: string;
};

const AddItemLink = ({ children, ...linkProps }: AddItemLinkProps) => (
  <ButtonLink {...linkProps} className={styles.link} variant="secondary" fullWidth>
    <Icon name="plus" size={16} /> {children}
  </ButtonLink>
);

export default AddItemLink;
